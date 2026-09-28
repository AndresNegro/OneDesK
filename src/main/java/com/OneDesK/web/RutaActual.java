package com.OneDesK.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Deja la direccion de la pagina en el modelo para que el selector de idioma vuelva a la misma
 * pagina: el link es la ruta actual con ?idioma=es o ?idioma=en. Thymeleaf 3.1 ya no da acceso al
 * pedido desde la plantilla, asi que la ruta se la pasamos nosotros.
 */
@ControllerAdvice
public class RutaActual {

	@ModelAttribute("rutaActual")
	public String rutaActual(HttpServletRequest pedido) {
		return pedido.getRequestURI();
	}
}
