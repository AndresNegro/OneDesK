package com.OneDesK.services;

import com.OneDesK.modelo.Producto;

public interface ProductoService {

	/** Da de alta un producto sin stock: el stock entra despues, con las cosechas. */
	public Producto crearProducto(String genetica, int precio);

	/** Las compras ya hechas conservan el precio al que se compraron. */
	public Producto cambiarPrecio(int productoId, int nuevoPrecio);
}
