package com.OneDesK.web;

import java.net.URI;

import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.OneDesK.excepciones.CredencialesInvalidasException;
import com.OneDesK.excepciones.EmailDuplicadoException;
import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.excepciones.StockInsuficienteException;
import com.OneDesK.excepciones.TopeCreditoExcedidoException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Cuando una regla de negocio rechaza una accion, la persona vuelve a la pagina en la que estaba
 * con el motivo arriba, en lugar de ver una pagina de error. Los controllers no repiten try/catch.
 */
@ControllerAdvice
public class ManejoDeErrores {

	@ExceptionHandler({ OperacionInvalidaException.class, RecursoNoEncontradoException.class,
			StockInsuficienteException.class, TopeCreditoExcedidoException.class, EmailDuplicadoException.class,
			CredencialesInvalidasException.class, IllegalArgumentException.class })
	public String reglaDeNegocio(RuntimeException error, HttpServletRequest pedido, RedirectAttributes flash) {
		flash.addFlashAttribute("error", error.getMessage());
		return "redirect:" + paginaAnterior(pedido);
	}

	// un campo vacio o con letras donde va un numero
	@ExceptionHandler({ MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class })
	public String datosIncompletos(HttpServletRequest pedido, RedirectAttributes flash) {
		flash.addFlashAttribute("error", "Completá todos los campos con valores válidos");
		return "redirect:" + paginaAnterior(pedido);
	}

	// solo se usa la ruta de la pagina anterior, nunca el dominio: asi no se puede redirigir a otro sitio
	private String paginaAnterior(HttpServletRequest pedido) {
		String referer = pedido.getHeader("Referer");
		if (referer == null) {
			return "/";
		}
		try {
			URI uri = URI.create(referer);
			String ruta = uri.getRawPath();
			if (ruta == null || ruta.isBlank() || !ruta.startsWith("/")) {
				return "/";
			}
			return uri.getRawQuery() == null ? ruta : ruta + "?" + uri.getRawQuery();
		} catch (IllegalArgumentException e) {
			return "/";
		}
	}
}
