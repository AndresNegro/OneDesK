package com.OneDesK.seguridad;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import com.OneDesK.web.AccesoController;
import com.OneDesK.web.PaginaDeInicio;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cuando alguien que ya entro pide una pagina que no le toca, no se le muestra un 403: se lo manda
 * a la pagina de inicio de su rol, que es lo unico que le sirve.
 */
@Component
public class AccesoDenegado implements AccessDeniedHandler {

	@Override
	public void handle(HttpServletRequest pedido, HttpServletResponse respuesta, AccessDeniedException negado)
			throws IOException, ServletException {
		// un pedido sin el token CSRF no es alguien equivocandose de pagina: no se lo acompania a
		// ningun lado, se lo rechaza
		if (negado instanceof CsrfException) {
			respuesta.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		Authentication quienPide = SecurityContextHolder.getContext().getAuthentication();
		String aDondeVa = AccesoController.LOGIN_URL;
		if (quienPide != null && quienPide.getPrincipal() instanceof DetallesDePersona persona) {
			aDondeVa = PaginaDeInicio.de(persona.getRol());
		}
		respuesta.sendRedirect(pedido.getContextPath() + aDondeVa);
	}
}
