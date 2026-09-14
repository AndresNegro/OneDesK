package com.OneDesK.services;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

		Map<Integer, Integer> cantidadPorProducto = new LinkedHashMap<>();
		for (LineaCompra linea : lineas) {
			if (linea.cantidad() <= 0) {
				throw new OperacionInvalidaException(
						"La cantidad del producto " + linea.productoId() + " debe ser mayor a cero");
			}
			cantidadPorProducto.merge(linea.productoId(), linea.cantidad(), Integer::sum);
		}

		Map<Producto, Integer> pedido = new LinkedHashMap<>();
		int total = 0;
		for (Map.Entry<Integer, Integer> entrada : cantidadPorProducto.entrySet()) {
			Producto producto = productoRepository.findById(entrada.getKey())
					.orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + entrada.getKey()));
			int cantidad = entrada.getValue();
			if (producto.getStock() < cantidad) {
				throw new StockInsuficienteException("Stock insuficiente de " + producto.getGenetica() + ": hay "
						+ producto.getStock() + " y se piden " + cantidad);
			}
			pedido.put(producto, cantidad);
			total += producto.getPrecio() * cantidad;
		}

		if (!pagado) {
			usuario.recalcularDeuda();
			int deudaResultante = usuario.getDeuda().getMonto() + total;
			if (deudaResultante > usuario.getTopeCredito()) {
				throw new TopeCreditoExcedidoException("La compra deja al usuario " + usuarioId + " con una deuda de "
						+ deudaResultante + " y su tope es " + usuario.getTopeCredito());
			}
		}

		Compra compra = new Compra(LocalDate.now(), pagado, usuario);
		for (Map.Entry<Producto, Integer> entrada : pedido.entrySet()) {
			Producto producto = entrada.getKey();
			int cantidad = entrada.getValue();
			producto.descontarStock(cantidad);
			compra.addItem(new ItemCompra(producto, cantidad));
		}
		usuario.agregarCompra(compra);

		return repositorio.save(compra);
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
}
