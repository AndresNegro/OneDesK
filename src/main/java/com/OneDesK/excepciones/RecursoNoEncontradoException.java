package com.OneDesK.excepciones;

public class RecursoNoEncontradoException extends ExcepcionDeNegocio {

	public RecursoNoEncontradoException(String clave, Object... argumentos) {
		super(clave, argumentos);
	}
}
