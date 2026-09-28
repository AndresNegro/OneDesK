package com.OneDesK.web.formularios;

/** Sumar o restar gramos al stock de un producto. */
public class FormularioDeAjuste {

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
