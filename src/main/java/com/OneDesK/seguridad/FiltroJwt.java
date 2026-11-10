package com.OneDesK.seguridad;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Mira el token que viene en cada pedido a la API y, si es valido, deja dicho quien lo mando. No
 * rechaza a nadie: si no hay token o no sirve, el pedido sigue sin autenticar y lo frena despues la
 * regla de autorizacion de esa direccion.
 */
@Component
public class FiltroJwt extends OncePerRequestFilter {

	private static final String CABECERA = "Authorization";
	private static final String PREFIJO = "Bearer ";

	@Autowired
	private JwtService jwtService;
	@Autowired
	private DetallesDePersonaService personas;

	@Override
	protected void doFilterInternal(HttpServletRequest pedido, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		String email = emailDelToken(pedido);
		if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			autenticar(email, pedido);
		}
		cadena.doFilter(pedido, respuesta);
	}

	private void autenticar(String email, HttpServletRequest pedido) {
		try {
			UserDetails quienEntro = personas.loadUserByUsername(email);
			// el token sigue valiendo, pero la cuenta se pudo dar de baja o quedar sin aprobar
			if (quienEntro.isEnabled()) {
				SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
						quienEntro, null, quienEntro.getAuthorities()));
			}
		} catch (UsernameNotFoundException cuentaBorrada) {
			SecurityContextHolder.clearContext();
		}
	}

	private String emailDelToken(HttpServletRequest pedido) {
		String cabecera = pedido.getHeader(CABECERA);
		if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
			return null;
		}
		return jwtService.emailDe(cabecera.substring(PREFIJO.length()).trim());
	}
}
