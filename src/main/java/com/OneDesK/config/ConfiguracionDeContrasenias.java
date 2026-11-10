package com.OneDesK.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Con que se encriptan las contrasenias. Esta aparte de ConfiguracionDeSeguridad porque no es cosa
 * de la web: los services lo usan al crear una persona, y los tests que levantan solo la capa de
 * datos lo importan sin tener que levantar toda la seguridad.
 */
@Configuration
public class ConfiguracionDeContrasenias {

	/** BCrypt: de la base no se puede sacar ninguna contrasenia, ni siquiera siendo el administrador. */
	@Bean
	public PasswordEncoder encriptador() {
		return new BCryptPasswordEncoder();
	}
}
