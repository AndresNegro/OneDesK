package com.OneDesK.excepciones;

public class OperacionInvalidaException extends ExcepcionDeNegocio {

	public OperacionInvalidaException(String clave, Object... argumentos) {
		super(clave, argumentos);
	}
}
