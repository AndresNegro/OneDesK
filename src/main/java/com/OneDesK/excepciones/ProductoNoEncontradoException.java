package com.OneDesK.excepciones;

// Subclase de RecursoNoEncontrado que ademas dice que genetica falta, para poder ofrecer darla de alta
public class ProductoNoEncontradoException extends RecursoNoEncontradoException {

	private final String genetica;

	public ProductoNoEncontradoException(String genetica) {
		super("error.producto.no.encontrado", genetica);
		this.genetica = genetica;
	}

	public String getGenetica() {
		return genetica;
	}
}
