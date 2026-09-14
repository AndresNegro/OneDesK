package com.OneDesK.excepciones;

public class EmailDuplicadoException extends RuntimeException {

	public EmailDuplicadoException(String mensaje) {
		super(mensaje);
	}
}
