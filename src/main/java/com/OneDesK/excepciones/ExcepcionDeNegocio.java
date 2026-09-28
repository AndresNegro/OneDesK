package com.OneDesK.excepciones;

import java.util.Arrays;

/**
 * La base de las excepciones de negocio. No lleva el texto del error sino la clave con la que se
 * busca en messages.properties, y los datos que van adentro del mensaje (el nombre del indoor, los
 * gramos que faltan). Asi el mismo error se muestra en el idioma de quien lo ve, y el texto vive en
 * un solo lugar: ManejoDeErrores lo traduce antes de mostrarlo.
 */
public abstract class ExcepcionDeNegocio extends RuntimeException implements MensajeTraducible {

	private final String clave;
	private final transient Object[] argumentos;

	protected ExcepcionDeNegocio(String clave, Object... argumentos) {
		// en el log y en los tests se ve la clave con sus datos, que es lo que identifica al error
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
