package com.OneDesK.excepciones;

public class StockInsuficienteException extends RuntimeException {

	public StockInsuficienteException(String mensaje) {
		super(mensaje);
	}
}
