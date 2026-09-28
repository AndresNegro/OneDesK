package com.OneDesK.web;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

	/** El parametro con el que se cambia de idioma: /catalogo?idioma=en */
	public static final String PARAMETRO_IDIOMA = "idioma";

	@Autowired
	private ControlDeAcceso controlDeAcceso;

	// el idioma elegido vive en la sesion: se elige una vez y vale para todas las paginas
	@Bean
	public LocaleResolver localeResolver() {
		SessionLocaleResolver resolutor = new SessionLocaleResolver();
		resolutor.setDefaultLocale(Locale.forLanguageTag("es"));
		return resolutor;
	}

	@Bean
	public LocaleChangeInterceptor cambioDeIdioma() {
		LocaleChangeInterceptor cambio = new LocaleChangeInterceptor();
		cambio.setParamName(PARAMETRO_IDIOMA);
		return cambio;
	}

	// el login, el registro, el css y las imagenes quedan abiertos; el resto pide sesion y rol
	@Override
	public void addInterceptors(InterceptorRegistry registro) {
		// el idioma se puede cambiar en cualquier pagina, incluso sin haber ingresado
		registro.addInterceptor(cambioDeIdioma());
		registro.addInterceptor(controlDeAcceso)
				.addPathPatterns(bajoLaRuta(CatalogoController.CATALOGO_URL), bajoLaRuta(CatalogoController.CARRITO_URL),
						bajoLaRuta(MisComprasController.MIS_COMPRAS_URL), bajoLaRuta(EmpleadoController.EMPLEADO_URL),
						bajoLaRuta(AdminController.ADMIN_URL));
	}

	// la pagina y todo lo que cuelga de ella: /admin y /admin/lo-que-sea
	private String bajoLaRuta(String url) {
		return url + "/**";
	}
}
