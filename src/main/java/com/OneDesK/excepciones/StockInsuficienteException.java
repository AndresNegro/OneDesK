package com.OneDesK.excepciones;

public class StockInsuficienteException extends ExcepcionDeNegocio {

	public StockInsuficienteException(String clave, Object... argumentos) {
		super(clave, argumentos);
	}
}
