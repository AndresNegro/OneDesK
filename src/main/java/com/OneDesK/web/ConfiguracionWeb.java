package com.OneDesK.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

	@Autowired
	private ControlDeAcceso controlDeAcceso;

	// el login, el registro, el css y las imagenes quedan abiertos; el resto pide sesion y rol
	@Override
	public void addInterceptors(InterceptorRegistry registro) {
		registro.addInterceptor(controlDeAcceso)
				.addPathPatterns("/catalogo/**", "/carrito/**", "/mis-compras/**", "/empleado/**", "/admin/**");
	}
}
