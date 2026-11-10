package com.OneDesK.seguridad;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.OneDesK.web.PaginaDeInicio;
import com.OneDesK.web.Sesion;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lo que pasa apenas alguien entra: se guardan en la sesion los datos que las paginas usan en cada
 * pedido (el id y el nombre) y se lo manda a la pagina de inicio de su rol. El id de sesion lo
 * renueva Spring Security por su cuenta, antes de llegar aca.
 */
@Component
public class ExitoAlIngresar implements AuthenticationSuccessHandler {

	@Override
	public void onAuthenticationSuccess(HttpServletRequest pedido, HttpServletResponse respuesta,
			Authentication autenticacion) throws IOException, ServletException {
		DetallesDePersona quienEntro = (DetallesDePersona) autenticacion.getPrincipal();
		Sesion.iniciar(pedido.getSession(), quienEntro);
		respuesta.sendRedirect(pedido.getContextPath() + PaginaDeInicio.de(quienEntro.getRol()));
	}
}
