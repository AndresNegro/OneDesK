package com.OneDesK;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.OneDesK.dtos.TokenDTO;
import com.OneDesK.web.formularios.FormularioDeIngreso;
import com.OneDesK.web.rest.AutenticacionAPI;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * La base de los tests que prueban la aplicacion por HTTP, como la usaria otro programa: levanta
 * la aplicacion entera en un puerto libre y deja listo el cliente para pegarle.
 *
 * No hace falta un perfil de test: la base de datos de los tests ya la fuerza BaseDeTest, que vale
 * para todos los contextos. La generacion de eventos se apaga: no tiene nada que ver con la API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "onedesk.eventos.activo=false" })
public abstract class BaseContextTest {

	protected static final String LOCAL_SERVER = "http://localhost";

	@LocalServerPort
	private int puerto;
	@Autowired
	private ObjectMapper lectorDeJson;

	private String token;

	protected int puerto() {
		return this.puerto;
	}

	protected URI uri(String ruta) {
		return URI.create(LOCAL_SERVER + ":" + puerto() + ruta);
	}

	/** Un GET que dice que habla JSON, que es lo que piden las APIs (ver el consumes). */
	protected HttpResponse<String> get(String ruta) {
		return enviar(HttpRequest.newBuilder(uri(ruta))
				.GET()
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
	}

	/** Un GET pidiendo otro formato en el Accept, para probar que la API contesta 406. */
	protected HttpResponse<String> getPidiendo(String ruta, MediaType formato) {
		return enviar(HttpRequest.newBuilder(uri(ruta))
				.GET()
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.header(HttpHeaders.ACCEPT, formato.toString()));
	}

	/** Un GET sin decir el formato, como lo manda el navegador. */
	protected HttpResponse<String> getSinFormato(String ruta) {
		return enviar(HttpRequest.newBuilder(uri(ruta)).GET());
	}

	protected HttpResponse<String> post(String ruta, String json) {
		return enviar(HttpRequest.newBuilder(uri(ruta))
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)));
	}

	/**
	 * Lee el JSON que contesto la API y lo convierte en los objetos que correspondan. Asi los tests
	 * preguntan por los datos (`dto.getEmail()`) y no por pedazos de texto dentro del JSON.
	 */
	protected <T> List<T> leerLista(HttpResponse<String> respuesta, Class<T> clase) {
		try {
			return lectorDeJson.readerForListOf(clase).readValue(respuesta.body());
		} catch (JsonProcessingException e) {
			throw new AssertionError("No se pudo leer el JSON: " + respuesta.body(), e);
		}
	}

	/** El cuerpo de un POST, armado desde el formulario y no escrito a mano como texto. */
	protected String comoJson(Object objeto) {
		try {
			return lectorDeJson.writeValueAsString(objeto);
		} catch (JsonProcessingException e) {
			throw new AssertionError("No se pudo armar el JSON de " + objeto, e);
		}
	}

	/**
	 * Entra a la API y se queda con el token: desde aca, todos los pedidos de este test lo mandan en
	 * la cabecera Authorization, igual que haria el programa que la consume.
	 */
	protected void entrarComo(String email, String contrasenia) {
		FormularioDeIngreso credenciales = new FormularioDeIngreso();
		credenciales.setEmail(email);
		credenciales.setContrasenia(contrasenia);

		HttpResponse<String> respuesta = post(AutenticacionAPI.LOGIN_URL, comoJson(credenciales));
		if (respuesta.statusCode() != 200) {
			throw new AssertionError("No se pudo entrar a la API: " + respuesta.statusCode() + " " + respuesta.body());
		}
		this.token = leer(respuesta, TokenDTO.class).getToken();
	}

	/** Sigue con otro token, para probar uno falso o cambiado. */
	protected void conToken(String token) {
		this.token = token;
	}

	/** Deja de mandar el token, para probar que sin el la API no contesta. */
	protected void salir() {
		this.token = null;
	}

	protected String getToken() {
		return this.token;
	}

	protected <T> T leer(HttpResponse<String> respuesta, Class<T> clase) {
		try {
			return lectorDeJson.readValue(respuesta.body(), clase);
		} catch (JsonProcessingException e) {
			throw new AssertionError("No se pudo leer el JSON: " + respuesta.body(), e);
		}
	}

	private HttpResponse<String> enviar(HttpRequest.Builder pedido) {
		if (this.token != null) {
			pedido.header(HttpHeaders.AUTHORIZATION, "Bearer " + this.token);
		}
		try {
			return HttpClient.newHttpClient().send(pedido.build(),
					HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new AssertionError("No se pudo conectar con la aplicacion", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Pedido interrumpido", e);
		}
	}
}
