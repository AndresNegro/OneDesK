package com.OneDesK.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.excepciones.StockInsuficienteException;
import com.OneDesK.excepciones.TopeCreditoExcedidoException;
import com.OneDesK.modelo.Compra;
import com.OneDesK.modelo.EstadoCompra;
import com.OneDesK.modelo.ItemCompra;
import com.OneDesK.modelo.Producto;
import com.OneDesK.modelo.Usuario;
import com.OneDesK.repositories.CompraRepository;
import com.OneDesK.repositories.ProductoRepository;
import com.OneDesK.repositories.UsuarioRepository;

@Service
public class CompraServiceImpl implements CompraService {

	@Autowired
	private CompraRepository repositorio;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private ProductoRepository productoRepository;
	@Autowired
	private LimitesDeCompraService limitesDeCompra;

	@Override
	@Transactional
	public Compra realizarCompra(int usuarioId, List<LineaCompra> lineas, boolean pagado) {
		if (lineas == null || lineas.isEmpty()) {
			throw new OperacionInvalidaException("La compra debe tener al menos un item");
		}

		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + usuarioId));
		if (!usuario.isAprobado()) {
			throw new OperacionInvalidaException("El usuario " + usuarioId + " todavia no fue aprobado");
		}

		List<ItemPedido> pedido = armarPedido(lineas);
		limitesDeCompra.obtener().verificar(gramos(pedido));
		int total = validarStockYCalcularTotal(pedido);

		// en cuenta corriente cuenta lo que ya debe y lo que pidio a cuenta y todavia espera aprobacion
		if (!pagado) {
			verificarTope(usuario, usuario.deudaComprometida() + total);
		}

		// el stock se reserva ahora: si el administrador la rechaza, vuelve
		Compra compra = Compra.pedida(LocalDate.now(), usuario, pagado);
		for (ItemPedido item : pedido) {
			item.getProducto().descontarStock(item.getCantidad());
			compra.addItem(new ItemCompra(item.getProducto(), item.getCantidad()));
		}
		usuario.agregarCompra(compra);

		return repositorio.save(compra);
	}

	/** Resuelve cada linea contra el catalogo, juntando las que repiten producto. */
	private List<ItemPedido> armarPedido(List<LineaCompra> lineas) {
		List<ItemPedido> pedido = new ArrayList<>();
		for (LineaCompra linea : lineas) {
			if (linea.cantidad() <= 0) {
				throw new OperacionInvalidaException(
						"La cantidad del producto " + linea.productoId() + " debe ser mayor a cero");
			}
			ItemPedido repetido = buscarPorProducto(pedido, linea.productoId());
			if (repetido != null) {
				repetido.sumarCantidad(linea.cantidad());
			} else {
				Producto producto = productoRepository.findById(linea.productoId())
						.orElseThrow(() -> new RecursoNoEncontradoException(
								"No existe el producto " + linea.productoId()));
				pedido.add(new ItemPedido(linea.productoId(), producto, linea.cantidad()));
			}
		}
		return pedido;
	}

	private ItemPedido buscarPorProducto(List<ItemPedido> pedido, int productoId) {
		for (ItemPedido item : pedido) {
			if (item.getProductoId() == productoId) {
				return item;
			}
		}
		return null;
	}

	/** Recorre todo el pedido antes de tocar nada, para que un item sin stock no deje descuentos a medias. */
	private void verificarTope(Usuario usuario, int deudaResultante) {
		if (deudaResultante > usuario.getTopeCredito()) {
			throw new TopeCreditoExcedidoException("La compra deja al usuario " + usuario.getId()
					+ " con una deuda de " + deudaResultante + " y su tope es " + usuario.getTopeCredito());
		}
	}

	private int gramos(List<ItemPedido> pedido) {
		int gramos = 0;
		for (ItemPedido item : pedido) {
			gramos += item.getCantidad();
		}
		return gramos;
	}

	private int validarStockYCalcularTotal(List<ItemPedido> pedido) {
		int total = 0;
		for (ItemPedido item : pedido) {
			Producto producto = item.getProducto();
			if (producto.getStock() < item.getCantidad()) {
				throw new StockInsuficienteException("Stock insuficiente de " + producto.getGenetica() + ": hay "
						+ producto.getStock() + " y se piden " + item.getCantidad());
			}
			total += producto.getPrecio() * item.getCantidad();
		}
		return total;
	}

	@Override
	@Transactional
	public Compra aprobarCompra(int compraId) {
		Compra compra = buscarCompra(compraId);
		Usuario usuario = compra.getUsuario();
		// el tope se vuelve a mirar: entre el pedido y la aprobacion pudo haber cambiado
		if (compra.isPendiente() && !compra.isPagaAlAprobar()) {
			usuario.recalcularDeuda();
			verificarTope(usuario, usuario.getDeuda().getMonto() + compra.getPrecio());
		}
		compra.aprobar();
		usuario.recalcularDeuda();
		return compra;
	}

	@Override
	@Transactional
	public Compra rechazarCompra(int compraId) {
		Compra compra = buscarCompra(compraId);
		compra.rechazar();
		for (ItemCompra item : compra.getItems()) {
			item.getProducto().reponerStock(item.getCantidad());
		}
		compra.getUsuario().recalcularDeuda();
		return compra;
	}

	@Override
	@Transactional
	public List<Compra> comprasPendientes() {
		return repositorio.findByEstadoOrderByFechaCompraAscIdAsc(EstadoCompra.PENDIENTE);
	}

	@Override
	@Transactional
	public Compra registrarPago(int compraId) {
		Compra compra = buscarCompra(compraId);
		if (compra.isPagado()) {
			throw new OperacionInvalidaException("La compra " + compraId + " ya esta pagada");
		}
		if (!compra.isAprobada()) {
			throw new OperacionInvalidaException("La compra " + compraId + " todavía no fue aprobada");
		}
		compra.getUsuario().registrarPago(compra);
		return compra;
	}

	@Override
	@Transactional
	public void anularCompra(int compraId) {
		Compra compra = buscarCompra(compraId);
		if (compra.isPagado()) {
			throw new OperacionInvalidaException("No se puede anular la compra " + compraId + " porque esta pagada");
		}
		if (compra.isRechazada()) {
			throw new OperacionInvalidaException("La compra " + compraId + " ya fue rechazada");
		}
		for (ItemCompra item : compra.getItems()) {
			item.getProducto().reponerStock(item.getCantidad());
		}
		Usuario usuario = compra.getUsuario();
		usuario.deleteCompra(compra);

		// el borrado se pide explicito y no se deja en manos del orphanRemoval de Usuario.compras:
		// si la compra se creo y se anulo dentro de la misma transaccion, la coleccion queda como estaba
		// y Hibernate no la ve como huerfana, pero la fila ya se inserto igual por el id autoincremental
		repositorio.delete(compra);
	}

	@Override
	@Transactional
	public List<Compra> comprasDe(int usuarioId) {
		return repositorio.findByUsuarioIdOrderByFechaCompraDescIdDesc(usuarioId);
	}

	@Override
	@Transactional
	public Compra registrarPagoDe(int usuarioId, int compraId) {
		verificarQueEsDelUsuario(usuarioId, compraId);
		return registrarPago(compraId);
	}

	@Override
	@Transactional
	public void anularCompraDe(int usuarioId, int compraId) {
		verificarQueEsDelUsuario(usuarioId, compraId);
		anularCompra(compraId);
	}

	// un usuario no puede pagar ni anular la compra de otro cambiando el id en la pagina
	private void verificarQueEsDelUsuario(int usuarioId, int compraId) {
		if (buscarCompra(compraId).getUsuario().getId() != usuarioId) {
			throw new OperacionInvalidaException("La compra " + compraId + " no es tuya");
		}
	}

	private Compra buscarCompra(int compraId) {
		return repositorio.findById(compraId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe la compra " + compraId));
	}

	/** Producto ya resuelto del catalogo con la cantidad total pedida, antes de convertirse en ItemCompra. */
	private static class ItemPedido {

		private final int productoId;
		private final Producto producto;
		private int cantidad;

		ItemPedido(int productoId, Producto producto, int cantidad) {
			this.productoId = productoId;
			this.producto = producto;
			this.cantidad = cantidad;
		}

		int getProductoId() { return productoId; }
		Producto getProducto() { return producto; }
		int getCantidad() { return cantidad; }
		void sumarCantidad(int extra) { this.cantidad += extra; }
	}
}
