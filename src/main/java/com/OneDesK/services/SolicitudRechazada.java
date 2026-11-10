package com.OneDesK.services;

/**
 * Paso que la administracion rechazo una solicitud de registro. Lleva el nombre y el email adentro
 * porque para cuando se avisa, esa persona ya no esta en la base.
 */
public class SolicitudRechazada {

	private final String nombre;
	private final String email;

	public SolicitudRechazada(String nombre, String email) {
		this.nombre = nombre;
		this.email = email;
	}

	public String getNombre() {
		return nombre;
	}

	public String getEmail() {
		return email;
	}
}
