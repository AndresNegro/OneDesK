package com.OneDesK.web;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Corre antes de cada controller de las paginas privadas. Sin sesion manda al login;
 * con el rol equivocado manda a la pagina de inicio de su rol: un usuario no entra al panel
 * del empleado ni a la administracion aunque escriba la direccion a mano.
 */
@Component
public class ControlDeAcceso implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest pedido, HttpServletResponse respuesta, Object handler)
			throws Exception {
		HttpSession sesion = pedido.getSession(false);
		if (!Sesion.ingreso(sesion)) {
			respuesta.sendRedirect(pedido.getContextPath() + "/");
			return false;
		}
		Rol rol = Sesion.rol(sesion);
		if (rol != rolQuePide(pedido.getRequestURI().substring(pedido.getContextPath().length()))) {
			respuesta.sendRedirect(pedido.getContextPath() + rol.getInicio());
			return false;
		}
		return true;
	}

	private Rol rolQuePide(String ruta) {
		if (ruta.startsWith("/admin")) {
			return Rol.ADMINISTRADOR;
		}
		if (ruta.startsWith("/empleado")) {
			return Rol.EMPLEADO;
		}
		return Rol.USUARIO;
	}
}
