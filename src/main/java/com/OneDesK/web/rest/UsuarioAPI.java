package com.OneDesK.web.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.OneDesK.modelo.Usuario;
import com.OneDesK.services.UsuarioService;

/** Los clientes aprobados, en JSON. La contrasenia no viaja nunca (ver Persona). */
@RestController
public class UsuarioAPI {

	public static final String LISTAR_USUARIOS_URL = "/api/usuarios";

	@Autowired
	private UsuarioService servicio;

	@GetMapping(value = LISTAR_USUARIOS_URL,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Usuario>> listar() {
		List<Usuario> usuarios = this.servicio.listarAprobados();

		return ResponseEntity.ok(usuarios);
	}
}
