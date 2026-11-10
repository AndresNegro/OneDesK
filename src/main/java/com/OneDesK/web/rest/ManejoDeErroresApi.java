package com.OneDesK.web.rest;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.OneDesK.web.Mensajes;

/**
 * Los errores de la API se contestan con el codigo que corresponde y el motivo en texto, no con una
 * redireccion: del otro lado no hay un navegador que la siga.
 */
@RestControllerAdvice(basePackages = "com.OneDesK.web.rest")
public class ManejoDeErroresApi {

	@Autowired
	private Mensajes mensajes;

	/** Email o contrasenia que no dan, y la cuenta que todavia no fue aprobada. */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<String> noPudoEntrar(AuthenticationException fallo, Locale idioma) {
		String motivo = fallo instanceof DisabledException ? "error.cuenta.pendiente" : "error.credenciales";

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mensajes.de(motivo, idioma));
	}
}
