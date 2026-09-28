package com.OneDesK.excepciones;

import java.util.Arrays;

/**
 * Un dato que no cumple lo que pide el modelo: un nombre vacio, un precio en cero. Sigue siendo una
 * IllegalArgumentException, como el resto de las validaciones de los setters, pero ademas lleva su
 * clave para traducirla. Ver ExcepcionDeNegocio, que hace lo mismo con las reglas de negocio.
 */
public class DatoInvalidoException extends IllegalArgumentException implements MensajeTraducible {

	private final String clave;
	private final transient Object[] argumentos;

	public DatoInvalidoException(String clave, Object... argumentos) {
		super(clave + (argumentos.length == 0 ? "" : " " + Arrays.toString(argumentos)));
		this.clave = clave;
		this.argumentos = argumentos;
	}

	@Override
	public String getClave() {
		return clave;
	}

	@Override
	public Object[] getArgumentos() {
		return argumentos;
	}
}
