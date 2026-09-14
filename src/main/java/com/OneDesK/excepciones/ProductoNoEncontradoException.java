package com.OneDesK.excepciones;

// Subclase de RecursoNoEncontrado que ademas dice que genetica falta, para poder ofrecer darla de alta
public class ProductoNoEncontradoException extends RecursoNoEncontradoException {

	private final String genetica;

	public ProductoNoEncontradoException(String genetica) {
		super("No hay un producto en el catalogo para la genetica " + genetica
				+ ". Hay que darlo de alta antes de cosecharla");
		this.genetica = genetica;
	}

	public String getGenetica() {
		return genetica;
	}
}
