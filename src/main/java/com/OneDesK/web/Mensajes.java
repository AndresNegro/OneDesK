package com.OneDesK.web;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Busca los textos en messages.properties. Los controllers lo usan para los avisos de exito:
 * el idioma llega en cada pedido, asi que el mismo aviso sale en espanol o en ingles segun quien
 * lo haya pedido. Los errores los traduce ManejoDeErrores.
 */
@Component
public class Mensajes {

	@Autowired
	private MessageSource mensajes;

	public String de(String clave, Locale idioma, Object... argumentos) {
		return mensajes.getMessage(clave, argumentos, idioma);
	}
}
