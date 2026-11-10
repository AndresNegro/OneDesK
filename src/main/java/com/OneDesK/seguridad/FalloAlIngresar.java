package com.OneDesK.seguridad;

import java.io.IOException;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.OneDesK.web.AccesoController;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Por que no pudo entrar. Se distinguen dos casos nada mas: la cuenta que todavia no fue aprobada,
 * que tiene que enterarse, y todo el resto, que se contesta igual para no decir si el email existe.
 */
@Component
public class FalloAlIngresar implements AuthenticationFailureHandler {

	public static final String ERROR = "?error";
	public static final String PENDIENTE = "?pendiente";

	@Override
	public void onAuthenticationFailure(HttpServletRequest pedido, HttpServletResponse respuesta,
			AuthenticationException fallo) throws IOException, ServletException {
		String motivo = fallo instanceof DisabledException ? PENDIENTE : ERROR;
		respuesta.sendRedirect(pedido.getContextPath() + AccesoController.LOGIN_URL + motivo);
	}
}
