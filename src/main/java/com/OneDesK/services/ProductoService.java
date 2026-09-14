package com.OneDesK.services;

import com.OneDesK.modelo.Producto;

public interface ProductoService {

	/** Da de alta un producto sin stock: el stock entra despues, con las cosechas. */
	public Producto crearProducto(String genetica, int precio);
}
