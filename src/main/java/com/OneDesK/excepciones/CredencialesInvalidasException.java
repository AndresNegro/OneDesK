package com.OneDesK.excepciones;

// el mensaje es el mismo si falla el email o la contrasenia: no se le dice a nadie cual de los dos existe
public class CredencialesInvalidasException extends RuntimeException {

	public CredencialesInvalidasException() {
		super("Email o contraseña incorrectos");
	}
}
