package com.OneDesK.excepciones;

public class EmailDuplicadoException extends ExcepcionDeNegocio {

	public EmailDuplicadoException(String clave, Object... argumentos) {
		super(clave, argumentos);
	}
}
