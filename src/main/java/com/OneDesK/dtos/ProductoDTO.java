package com.OneDesK.dtos;

/** Una genetica del catalogo, como la ve quien consume la API. */
public class ProductoDTO extends BaseDTO {

	private String genetica;
	private int stock;
	private int precio;

	public String getGenetica() {
		return genetica;
	}

	public void setGenetica(String genetica) {
		this.genetica = genetica;
	}

	public int getStock() {
		return stock;
	}

	public void setStock(int stock) {
		this.stock = stock;
	}

	public int getPrecio() {
		return precio;
	}

	public void setPrecio(int precio) {
		this.precio = precio;
	}
}
