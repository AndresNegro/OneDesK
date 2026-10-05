package com.OneDesK.web;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
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

	/**
	 * El texto de esa clave, o el de la de respaldo si la primera no esta escrita. Sirve para los
	 * errores que arma Spring con sus propias claves, como una letra donde va un numero.
	 */
	public String deOSino(String clave, String respaldo, Locale idioma, Object... argumentos) {
		try {
			return mensajes.getMessage(clave, argumentos, idioma);
		} catch (NoSuchMessageException noEstaEscrita) {
			return mensajes.getMessage(respaldo, null, idioma);
		}
	}
}
