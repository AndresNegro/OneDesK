package com.OneDesK.web.formularios;

/** Cuantos gramos de un producto se agregan al carrito o quedan en el. */
public class FormularioDeCarrito {

	private int productoId;
	private int cantidad;

	public int getProductoId() {
		return productoId;
	}

	public void setProductoId(int productoId) {
		this.productoId = productoId;
	}

	public int getCantidad() {
		return cantidad;
	}

	public void setCantidad(int cantidad) {
		this.cantidad = cantidad;
	}
}
