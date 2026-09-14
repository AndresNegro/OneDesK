package com.OneDesK.services;

import java.util.List;

import com.OneDesK.modelo.Producto;

public interface ProductoService {

	/** Da de alta un producto sin stock: el stock entra despues, con las cosechas. */
	public Producto crearProducto(String genetica, int precio);

	/** Las compras ya hechas conservan el precio al que se compraron. */
	public Producto cambiarPrecio(int productoId, int nuevoPrecio);

	// Las consultas del catalogo devuelven solo productos con stock: lo que no tiene stock no se puede comprar

	public List<Producto> listarProductos();

	/** Busca por parte de la genetica sin distinguir mayusculas. Sin texto, devuelve todo el catalogo. */
	public List<Producto> buscarPorGenetica(String texto);

	/** Del mas barato al mas caro. */
	public List<Producto> listarPorPrecio();
}
