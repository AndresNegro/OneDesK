package com.OneDesK.web.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.OneDesK.dtos.DTO;
import com.OneDesK.services.UsuarioService;
import com.OneDesK.web.Mensajes;
import com.OneDesK.web.formularios.FormularioDeRegistro;
import com.OneDesK.web.validadores.ValidadorDeRegistro;

/** Los clientes aprobados, en JSON. La contrasenia no viaja nunca (ver Persona). */
@RestController
public class UsuarioAPI {

	public static final String LISTAR_USUARIOS_URL = "/api/usuarios";
	public static final String GUARDAR_USUARIO_URL = "/api/usuarios/register";

	@Autowired
	private UsuarioService servicio;
	@Autowired
	private ValidadorDeRegistro validador;
	@Autowired
	private Mensajes mensajes;

	@GetMapping(value = LISTAR_USUARIOS_URL,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<DTO>> listar() {
		List<DTO> usuarios = this.servicio.exponer();

		return ResponseEntity.ok(usuarios);
	}

	/**
	 * Pide una cuenta desde otro programa. Es el mismo circuito que el formulario de la pagina:
	 * la cuenta queda pendiente hasta que la administracion la apruebe.
	 *
	 * Las validaciones son las mismas que las de la pagina, con el mismo validador: aca no hay
	 * BindingResult, asi que se le pide a mano con validateObject.
	 */
	@PostMapping(value = GUARDAR_USUARIO_URL,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> registrar(@RequestBody FormularioDeRegistro form, Locale idioma) {
		ResponseEntity<String> respuesta = null;

		Errors errores = this.validador.validateObject(form);
		if (errores.hasErrors()) {
			respuesta = ResponseEntity.badRequest().body(motivos(errores, idioma));
		} else {
			this.servicio.registrar(form.getNombre(), form.getApellido(), form.getEmail(), form.getContrasenia());
			respuesta = ResponseEntity.status(HttpStatus.CREATED).build();
		}

		return respuesta;
	}

	/** Por que no se pudo, en el idioma de quien lo pidio y con un motivo por linea. */
	private String motivos(Errors errores, Locale idioma) {
		List<String> motivos = new ArrayList<>();
		for (ObjectError error : errores.getAllErrors()) {
			motivos.add(mensajes.deOSino(error.getCode(), "error.campos", idioma, error.getArguments()));
		}
		return String.join("\n", motivos);
	}
}
