package com.OneDesK.web;

import com.OneDesK.modelo.Administrador;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Persona;

/** Que tipo de persona ingreso, y a que pagina va al entrar. */
public enum Rol {

	USUARIO("/catalogo"),
	EMPLEADO("/empleado"),
	ADMINISTRADOR("/admin");

	private final String inicio;

	Rol(String inicio) {
		this.inicio = inicio;
	}

	public String getInicio() {
		return inicio;
	}

	public static Rol de(Persona persona) {
		if (persona instanceof Administrador) {
			return ADMINISTRADOR;
		}
		if (persona instanceof EmpleadoIndoor) {
			return EMPLEADO;
		}
		return USUARIO;
	}
}
