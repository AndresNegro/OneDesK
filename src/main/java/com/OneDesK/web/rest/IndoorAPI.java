package com.OneDesK.web.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.OneDesK.dtos.DTO;
import com.OneDesK.services.IndoorService;

/** Como viene cada indoor: cuanto tiene plantado y cuanto le falta atender. */
@RestController
public class IndoorAPI {

	public static final String LISTAR_INDOORS_URL = "/api/indoors";

	@Autowired
	private IndoorService servicio;

	@GetMapping(value = LISTAR_INDOORS_URL,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<DTO>> listar() {
		List<DTO> indoors = this.servicio.exponer();

		return ResponseEntity.ok(indoors);
	}
}
