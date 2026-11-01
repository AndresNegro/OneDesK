package com.OneDesK.web.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.OneDesK.dtos.DTO;
import com.OneDesK.services.UsuarioService;

/**
 * Los clientes con lo que ve la administracion: tambien las solicitudes sin aprobar, y de cada uno
 * lo comprometido y cuantas compras lleva. Es la misma lista que UsuarioAPI pero con mas datos,
 * por eso el DTO hereda del otro en lugar de repetirlo.
 */
@RestController
public class UsuarioAdminAPI {

	public static final String LISTAR_USUARIOS_ADMIN_URL = "/api/admin/usuarios";

	@Autowired
	private UsuarioService servicio;

	@GetMapping(value = LISTAR_USUARIOS_ADMIN_URL,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<DTO>> listar() {
		List<DTO> usuarios = this.servicio.exponerParaAdmin();

		return ResponseEntity.ok(usuarios);
	}
}
