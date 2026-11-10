package com.OneDesK.web;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hace de navegador en los tests: pedidos HTTP reales contra la aplicacion levantada, guardando la
 * cookie de sesion como un navegador de verdad y siguiendo las redirecciones. Cada persona del test
 * (usuario, empleado, administrador) usa su propio Navegador, asi cada una tiene su sesion.
 */
class Navegador {

	private static final String CAMPO_CSRF = "_csrf";
	private static final Pattern CSRF_EN_EL_HTML =
			Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");

	private final String base;
	private final CookieManager galletitas = new CookieManager();
	private final HttpClient cliente;
	private String paginaActual;
	private String html = "";

	Navegador(int puerto) {
		this.base = "http://localhost:" + puerto;
		this.cliente = HttpClient.newBuilder()
				.cookieHandler(galletitas)
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
	 * nombre, valor, nombre, valor. Manda de Referer la pagina actual, igual que un navegador, y el
	 * token CSRF que venia en el formulario de la pagina, sin el cual Spring Security rechaza el POST.
	 */
	String enviar(String ruta, String... campos) {
		StringBuilder cuerpo = new StringBuilder();
		agregar(cuerpo, CAMPO_CSRF, tokenCsrf());
		for (int i = 0; i < campos.length; i += 2) {
			agregar(cuerpo, campos[i], campos[i + 1]);
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

	/** El id de sesion que le dio la aplicacion, o null si todavia no tiene ninguna. */
	String idDeSesion() {
		return galletitas.getCookieStore().getCookies().stream()
				.filter(galletita -> "JSESSIONID".equals(galletita.getName()))
				.map(java.net.HttpCookie::getValue)
				.findFirst()
				.orElse(null);
	}

	/**
	 * Un POST sin el token CSRF, como lo mandaria un formulario puesto en otro sitio para que lo
	 * envie alguien que tiene la sesion abierta. Devuelve el codigo, que es lo que se quiere mirar.
	 */
	int enviarSinToken(String ruta, String... campos) {
		StringBuilder cuerpo = new StringBuilder();
		for (int i = 0; i < campos.length; i += 2) {
			agregar(cuerpo, campos[i], campos[i + 1]);
		}
		try {
			return cliente.send(HttpRequest.newBuilder(URI.create(base + ruta))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString())).build(),
					HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).statusCode();
		} catch (IOException e) {
			throw new AssertionError("No se pudo conectar con la aplicacion", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError("Pedido interrumpido", e);
		}
	}

	private void agregar(StringBuilder cuerpo, String campo, String valor) {
		if (cuerpo.length() > 0) {
			cuerpo.append('&');
		}
		cuerpo.append(URLEncoder.encode(campo, StandardCharsets.UTF_8)).append('=')
				.append(URLEncoder.encode(valor, StandardCharsets.UTF_8));
	}

	/**
	 * El token CSRF del formulario que se esta por enviar. Si la pagina actual no tiene ninguno
	 * (todavia no se abrio nada, por ejemplo), se abre el login para pedirlo, como haria cualquiera
	 * antes de completar un formulario.
	 */
	private String tokenCsrf() {
		String token = tokenEnElHtml();
		if (token == null) {
			abrir("/");
			token = tokenEnElHtml();
		}
		return token == null ? "" : token;
	}

	private String tokenEnElHtml() {
		Matcher token = CSRF_EN_EL_HTML.matcher(html);
		return token.find() ? token.group(1) : null;
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
