package com.OneDesK.web.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.OneDesK.modelo.Producto;
import com.OneDesK.services.ProductoService;

/**
 * El catalogo, para que lo lea otro programa. Devuelve JSON y no una pagina: por eso es
 * @RestController y no @Controller, y por eso vive en web.rest y no al lado de los otros.
 */
@RestController
public class ProductoAPI {

	public static final String LISTAR_PRODUCTOS_URL = "/api/productos";

	@Autowired
	private ProductoService servicio;

	/**
	 * Las geneticas con stock, que son las que se pueden comprar hoy.
	 *
	 * consumes y produces son los media types: en que formato se recibe y en que formato se contesta.
	 * Ojo con el consumes en un GET: el pedido no trae cuerpo, asi que solo pasan los clientes que
	 * igual mandan Content-Type, como Postman. Desde el navegador, esta direccion devuelve 415.
	 */
	@GetMapping(value = LISTAR_PRODUCTOS_URL,
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Producto>> listar() {
		List<Producto> productos = this.servicio.listarProductos();

		return ResponseEntity.ok(productos);
	}
}
