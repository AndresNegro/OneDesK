package com.OneDesK.helpers;

public class ValidationUtils {

	/** Exige algo antes de la @, una sola @, y un dominio con un punto que no quede en un extremo. */
	public static boolean isValidEmail(String email) {
		if (!tieneTexto(email)) {
			return false;
		}
		String limpio = email.trim();
		if (limpio.contains(" ")) {
			return false;
		}

		int arroba = limpio.indexOf('@');
		if (arroba <= 0) {
			return false;
		}
		if (arroba != limpio.lastIndexOf('@')) {
			return false;
		}

		String dominio = limpio.substring(arroba + 1);
		int punto = dominio.lastIndexOf('.');
		return punto > 0 && punto < dominio.length() - 1;
	}

	/** Devuelve true si el texto tiene al menos cant caracteres. */
	public static boolean tieneMasDe(String texto, int cant) {
		return texto != null && texto.length() >= cant;
	}

	public static boolean tieneTexto(String texto) {
		return texto != null && !texto.isBlank();
	}
}
