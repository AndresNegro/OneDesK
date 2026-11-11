package com.OneDesK.web;

import java.util.Locale;
import java.util.Set;

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

	// el idioma elegido vive en la sesion: se elige una vez y vale para todas las paginas
	@Bean
	public LocaleResolver localeResolver() {
		SessionLocaleResolver resolutor = new SessionLocaleResolver();
		resolutor.setDefaultLocale(Locale.forLanguageTag("es"));
		return resolutor;
	}

	/** Los idiomas que tienen archivo de mensajes. */
	public static final Set<String> IDIOMAS = Set.of("es", "en");

	// Solo se aceptan los idiomas que existen: cada idioma distinto queda guardado para siempre en
	// la cache de mensajes de Spring, y sin este filtro cualquiera podia llenarla con ?idioma=inventado
	@Bean
	public LocaleChangeInterceptor cambioDeIdioma() {
		LocaleChangeInterceptor cambio = new LocaleChangeInterceptor() {
			@Override
			protected Locale parseLocaleValue(String valor) {
				Locale pedido = super.parseLocaleValue(valor);
				return pedido != null && IDIOMAS.contains(pedido.getLanguage())
						? Locale.forLanguageTag(pedido.getLanguage())
						: null;
			}
		};
		cambio.setParamName(PARAMETRO_IDIOMA);
		cambio.setIgnoreInvalidLocale(true);
		return cambio;
	}

	// quien entra a cada pagina lo decide Spring Security (ver ConfiguracionDeSeguridad)
	@Override
	public void addInterceptors(InterceptorRegistry registro) {
		// el idioma se puede cambiar en cualquier pagina, incluso sin haber ingresado
		registro.addInterceptor(cambioDeIdioma());
	}
}
