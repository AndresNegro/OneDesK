package com.OneDesK.web;

import com.OneDesK.modelo.Persona;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Lo que se guarda en la sesion de quien ingreso: su id, su rol, su nombre y, si es usuario, su carrito.
 * Se guarda el id y no la entidad, asi cada pedido lee los datos actualizados de la base.
 */
public final class Sesion {

	private static final String PERSONA_ID = "personaId";
	private static final String ROL = "rol";
	private static final String NOMBRE = "nombre";
	private static final String CARRITO = "carrito";

	private Sesion() {
	}

	/**
	 * Abre la sesion de quien acaba de ingresar. Lo primero que hace es cambiarle el id: si alguien
	 * le fijo uno antes de que entrara (poniendolo en un link, por ejemplo), ese id deja de servir y
	 * no puede quedarse adentro de su sesion. Lo que ya habia guardado, como el idioma, se conserva.
	 */
	public static void iniciar(HttpServletRequest pedido, Persona persona) {
		pedido.getSession();
		pedido.changeSessionId();
		HttpSession sesion = pedido.getSession();
		sesion.setAttribute(PERSONA_ID, persona.getId());
		sesion.setAttribute(ROL, Rol.de(persona));
		sesion.setAttribute(NOMBRE, persona.getNombre() + " " + persona.getApellido());
	}

	public static boolean ingreso(HttpSession sesion) {
		return sesion != null && sesion.getAttribute(PERSONA_ID) != null;
	}

	public static int personaId(HttpSession sesion) {
		return (Integer) sesion.getAttribute(PERSONA_ID);
	}

	public static String nombre(HttpSession sesion) {
		return (String) sesion.getAttribute(NOMBRE);
	}

	public static Rol rol(HttpSession sesion) {
		return (Rol) sesion.getAttribute(ROL);
	}

	/** El carrito del usuario: se crea vacio la primera vez que se pide. */
	public static Carrito carrito(HttpSession sesion) {
		Carrito carrito = (Carrito) sesion.getAttribute(CARRITO);
		if (carrito == null) {
			carrito = new Carrito();
			sesion.setAttribute(CARRITO, carrito);
		}
		return carrito;
	}
}
