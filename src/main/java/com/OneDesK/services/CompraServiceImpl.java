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

	@Override
	@Transactional
	public Compra realizarCompra(int usuarioId, List<LineaCompra> lineas, boolean pagado) {
		if (lineas == null || lineas.isEmpty()) {
			throw new OperacionInvalidaException("La compra debe tener al menos un item");
		}

		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + usuarioId));

		List<ItemPedido> pedido = armarPedido(lineas);
		int total = validarStockYCalcularTotal(pedido);

		if (!pagado) {
			usuario.recalcularDeuda();
			int deudaResultante = usuario.getDeuda().getMonto() + total;
			if (deudaResultante > usuario.getTopeCredito()) {
				throw new TopeCreditoExcedidoException("La compra deja al usuario " + usuarioId + " con una deuda de "
						+ deudaResultante + " y su tope es " + usuario.getTopeCredito());
			}
		}

		Compra compra = new Compra(LocalDate.now(), pagado, usuario);
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
	public Compra registrarPago(int compraId) {
		Compra compra = buscarCompra(compraId);
		if (compra.isPagado()) {
			throw new OperacionInvalidaException("La compra " + compraId + " ya esta pagada");
		}
		compra.getUsuario().registrarPago(compra);
		return repositorio.save(compra);
	}

	@Override
	@Transactional
	public void anularCompra(int compraId) {
		Compra compra = buscarCompra(compraId);
		if (compra.isPagado()) {
			throw new OperacionInvalidaException("No se puede anular la compra " + compraId + " porque esta pagada");
		}
		for (ItemCompra item : compra.getItems()) {
			item.getProducto().reponerStock(item.getCantidad());
		}
		Usuario usuario = compra.getUsuario();
		usuario.deleteCompra(compra);
		usuarioRepository.save(usuario);
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
