package com.OneDesK.web.formularios;

/** El alta de una genetica en el catalogo, con su precio por gramo. */
public class FormularioDeProducto {

	private String genetica;
	private int precio;

	public String getGenetica() {
		return genetica;
	}

	public void setGenetica(String genetica) {
		this.genetica = genetica;
	}

	public int getPrecio() {
		return precio;
	}

	public void setPrecio(int precio) {
		this.precio = precio;
	}
}
