package com.OneDesK.services;

/** Paso que la administracion aprobo la cuenta de alguien. Lo escucha AvisosPorCorreo. */
public class CuentaAprobada {

	private final String nombre;
	private final String email;
	private final int topeCredito;

	public CuentaAprobada(String nombre, String email, int topeCredito) {
		this.nombre = nombre;
		this.email = email;
		this.topeCredito = topeCredito;
	}

	public String getNombre() {
		return nombre;
	}

	public String getEmail() {
		return email;
	}

	public int getTopeCredito() {
		return topeCredito;
	}
}
