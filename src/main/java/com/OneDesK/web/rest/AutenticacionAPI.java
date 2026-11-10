package com.OneDesK.web.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.OneDesK.dtos.TokenDTO;
import com.OneDesK.seguridad.DetallesDePersona;
import com.OneDesK.seguridad.JwtService;
import com.OneDesK.web.formularios.FormularioDeIngreso;

/**
 * El login de la API: se mandan email y contrasenia una sola vez y se recibe el token con el que se
 * piden las demas direcciones. Si las credenciales no dan, el AuthenticationManager tira la
 * excepcion y la respuesta es 401 (ver ManejoDeErroresApi).
 */
@RestController
public class AutenticacionAPI {

	public static final String LOGIN_URL = "/api/login";

	public static final String BEARER = "Bearer";

	@Autowired
	private AuthenticationManager autenticador;
	@Autowired
	private JwtService jwtService;

	@PostMapping(value = LOGIN_URL,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<TokenDTO> ingresar(@RequestBody FormularioDeIngreso form) {
		Authentication autenticada = this.autenticador.authenticate(
				new UsernamePasswordAuthenticationToken(form.getEmail(), form.getContrasenia()));
		DetallesDePersona quienEntro = (DetallesDePersona) autenticada.getPrincipal();

		return ResponseEntity.ok(new TokenDTO(this.jwtService.generar(quienEntro), BEARER,
				this.jwtService.getMinutosQueDura()));
	}
}
