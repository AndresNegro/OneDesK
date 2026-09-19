package com.OneDesK.web;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Hace de navegador en los tests: pedidos HTTP reales contra la aplicacion levantada, guardando la
 * cookie de sesion como un navegador de verdad y siguiendo las redirecciones. Cada persona del test
 * (usuario, empleado, administrador) usa su propio Navegador, asi cada una tiene su sesion.
 */
class Navegador {

	private final String base;
	private final HttpClient cliente;
	private String paginaActual;
	private String html = "";

	Navegador(int puerto) {
		this.base = "http://localhost:" + puerto;
		this.cliente = HttpClient.newBuilder()
				.cookieHandler(new CookieManager())
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		this.paginaActual = base + "/";
	}

	/** Abre una pagina y devuelve su HTML. */
	String abrir(String ruta) {
		return enviar(HttpRequest.newBuilder(URI.create(base + ruta)).GET());
	}

	/**
	 * Envia un formulario por POST, como el boton de una pagina. Los campos van de a pares:
	 * nombre, valor, nombre, valor. Manda de Referer la pagina actual, igual que un navegador.
	 */
	String enviar(String ruta, String... campos) {
		StringBuilder cuerpo = new StringBuilder();
		for (int i = 0; i < campos.length; i += 2) {
			if (cuerpo.length() > 0) {
				cuerpo.append('&');
			}
			cuerpo.append(URLEncoder.encode(campos[i], StandardCharsets.UTF_8)).append('=')
					.append(URLEncoder.encode(campos[i + 1], StandardCharsets.UTF_8));
		}
		return enviar(HttpRequest.newBuilder(URI.create(base + ruta))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("Referer", paginaActual)
				.POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString())));
	}

	/** La ruta en la que termino el ultimo pedido, despues de seguir las redirecciones. */
	String ruta() {
		return URI.create(paginaActual).getPath();
	}

	String html() {
		return html;
	}

	private String enviar(HttpRequest.Builder pedido) {
		try {
			HttpResponse<String> respuesta = cliente.send(pedido.build(),
					HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (respuesta.statusCode() >= 400) {
				throw new AssertionError("La pagina respondio " + respuesta.statusCode() + " en " + respuesta.uri()
						+ "\n" + respuesta.body());
			}
			paginaActual = respuesta.uri().toString();
			html = respuesta.body();
			return html;
		} catch (IOException e) {
			throw new AssertionError("No se pudo conectar con la aplicacion", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Pedido interrumpido", e);
		}
	}
}
