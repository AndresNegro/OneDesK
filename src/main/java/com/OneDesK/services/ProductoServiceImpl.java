package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.Producto;
import com.OneDesK.repositories.ProductoRepository;

@Service
public class ProductoServiceImpl implements ProductoService {

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
		Producto producto = repositorio.findById(productoId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto " + productoId));
		producto.setPrecio(nuevoPrecio);
		return producto;
	}
}
