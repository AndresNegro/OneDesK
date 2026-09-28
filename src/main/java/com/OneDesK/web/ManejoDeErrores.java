package com.OneDesK.web;

import java.net.URI;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.OneDesK.excepciones.CredencialesInvalidasException;
import com.OneDesK.excepciones.EmailDuplicadoException;
import com.OneDesK.excepciones.MensajeTraducible;
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
public class ManejoDeErrores extends BaseController {

	@Autowired
	private MessageSource mensajes;

	@ExceptionHandler({ OperacionInvalidaException.class, RecursoNoEncontradoException.class,
			StockInsuficienteException.class, TopeCreditoExcedidoException.class, EmailDuplicadoException.class,
			CredencialesInvalidasException.class, IllegalArgumentException.class })
	public String reglaDeNegocio(RuntimeException error, HttpServletRequest pedido, Locale idioma,
			RedirectAttributes flash) {
		flash.addFlashAttribute(ERROR, traducir(error, idioma));
		return redirect(paginaAnterior(pedido));
	}

	// los errores del negocio viajan con su clave y sus datos: el texto sale recien aca, en el idioma
	// de quien lo va a leer. Si alguno no la trae, se muestra tal cual vino.
	private String traducir(RuntimeException error, Locale idioma) {
		if (error instanceof MensajeTraducible traducible) {
			return mensajes.getMessage(traducible.getClave(), traducible.getArgumentos(), error.getMessage(), idioma);
		}
		return error.getMessage();
	}

	// un campo vacio o con letras donde va un numero. BindException es el mismo caso pero en los
	// formularios: cuando un campo no entra en el tipo del Formulario, Spring no llama al controller
	@ExceptionHandler({ MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
			BindException.class })
	public String datosIncompletos(HttpServletRequest pedido, Locale idioma, RedirectAttributes flash) {
		flash.addFlashAttribute(ERROR, mensajes.getMessage("error.campos", null, idioma));
		return redirect(paginaAnterior(pedido));
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
