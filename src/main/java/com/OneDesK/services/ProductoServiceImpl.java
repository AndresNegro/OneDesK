package com.OneDesK.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.Producto;
import com.OneDesK.repositories.ProductoRepository;

@Service
public class ProductoServiceImpl implements ProductoService {

	private static final int SIN_STOCK = 0;

	@Autowired
	private ProductoRepository repositorio;

	@Override
	@Transactional
	public Producto crearProducto(String genetica, int precio) {
		Producto producto = new Producto(genetica, 0, precio);

		if (repositorio.existsByGeneticaIgnoreCase(producto.getGenetica())) {
			throw new OperacionInvalidaException("Ya existe un producto con la genetica " + producto.getGenetica());
		}
		return repositorio.save(producto);
	}

	@Override
	@Transactional
	public Producto cambiarPrecio(int productoId, int nuevoPrecio) {
		Producto producto = buscarProducto(productoId);
		producto.setPrecio(nuevoPrecio);
		return producto;
	}

	@Override
	@Transactional
	public Producto fijarStock(int productoId, int stock) {
		Producto producto = buscarProducto(productoId);
		producto.fijarStock(stock);
		return producto;
	}

	@Override
	@Transactional
	public Producto ajustarStock(int productoId, int cantidad) {
		Producto producto = buscarProducto(productoId);
		producto.ajustarStock(cantidad);
		return producto;
	}

	@Override
	@Transactional
	public List<Producto> listarProductos() {
		return repositorio.findByStockGreaterThanOrderByGeneticaAsc(SIN_STOCK);
	}

	@Override
	@Transactional
	public List<Producto> buscarPorGenetica(String texto) {
		if (texto == null || texto.isBlank()) {
			return listarProductos();
		}
		return repositorio.findByGeneticaContainingIgnoreCaseAndStockGreaterThanOrderByGeneticaAsc(texto.trim(),
				SIN_STOCK);
	}

	@Override
	@Transactional
	public List<Producto> listarPorPrecio() {
		return repositorio.findByStockGreaterThanOrderByPrecioAsc(SIN_STOCK);
	}

	private Producto buscarProducto(int productoId) {
		return repositorio.findById(productoId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + productoId));
	}
}
