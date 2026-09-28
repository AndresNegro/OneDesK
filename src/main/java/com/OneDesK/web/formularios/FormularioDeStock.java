package com.OneDesK.web.formularios;

/** El stock contado de un producto: se deja en ese numero. */
public class FormularioDeStock {

	private int productoId;
	private int stock;

	public int getProductoId() {
		return productoId;
	}

	public void setProductoId(int productoId) {
		this.productoId = productoId;
	}

	public int getStock() {
		return stock;
	}

	public void setStock(int stock) {
		this.stock = stock;
	}
}
