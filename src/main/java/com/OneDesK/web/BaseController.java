package com.OneDesK.web;

/**
 * Lo que comparten todos los controllers: el nombre con el que viaja el formulario a la plantilla,
 * los de los dos avisos, y armar la redireccion sin repetir el prefijo en cada return. Un nombre
 * escrito a mano en veinte lugares es un error de tipeo esperando: aca esta escrito una sola vez.
 */
public abstract class BaseController {

	/** Con este nombre la plantilla encuentra el formulario en th:object="${form}". */
	protected static final String FORM_ATTRIBUTE = "form";

	/** Los avisos que muestra el fragmento avisos: en verde y en rosa. */
	protected static final String EXITO = "exito";
	protected static final String ERROR = "error";

	protected String redirect(String ruta) {
		return "redirect:" + ruta;
	}
}
