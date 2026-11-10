package com.OneDesK.web;

import com.OneDesK.enums.Rol;
import com.OneDesK.seguridad.DetallesDePersona;

import jakarta.servlet.http.HttpSession;

/**
 * Lo que se guarda en la sesion de quien ingreso: su id, su rol, su nombre y, si es usuario, su
 * carrito. Se guarda el id y no la entidad, asi cada pedido lee los datos actualizados de la base.
 * Quien entra y que puede hacer lo decide Spring Security; esto es solo lo que las paginas releen
 * en cada pedido.
 */
public final class Sesion {

	private static final String PERSONA_ID = "personaId";
	private static final String ROL = "rol";
	private static final String NOMBRE = "nombre";
	private static final String CARRITO = "carrito";

	private Sesion() {
	}

	/** Guarda los datos de quien acaba de ingresar. Lo llama ExitoAlIngresar, nadie mas. */
	public static void iniciar(HttpSession sesion, DetallesDePersona quienEntro) {
		sesion.setAttribute(PERSONA_ID, quienEntro.getId());
		sesion.setAttribute(ROL, quienEntro.getRol());
		sesion.setAttribute(NOMBRE, quienEntro.getNombreCompleto());
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
