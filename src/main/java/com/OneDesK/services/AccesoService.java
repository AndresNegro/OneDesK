package com.OneDesK.services;

import com.OneDesK.modelo.Persona;

public interface AccesoService {

	/**
	 * Devuelve la persona con ese email y contrasenia: un usuario, un empleado o un administrador.
	 * Falla con CredencialesInvalidasException si el email no existe o la contrasenia no coincide,
	 * y con OperacionInvalidaException si es un usuario que el administrador todavia no aprobo.
	 */
	public Persona ingresar(String email, String contrasenia);
}
