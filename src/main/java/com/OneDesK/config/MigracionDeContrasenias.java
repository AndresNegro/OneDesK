package com.OneDesK.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.modelo.Persona;
import com.OneDesK.repositories.PersonaRepository;

/**
 * Hasta que se sumo Spring Security las contrasenias estaban guardadas tal cual se escribian. Esta
 * tarea las encripta al arrancar, para que nadie pierda su cuenta por el cambio. Es idempotente:
 * las que ya estan encriptadas no se tocan, asi que puede quedar prendida sin hacer dano.
 */
@Component
@Order(0)
public class MigracionDeContrasenias implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MigracionDeContrasenias.class);

	@Autowired
	private PersonaRepository repositorio;
	@Autowired
	private PasswordEncoder encriptador;

	@Value("${onedesk.contrasenias.migrar:true}")
	private boolean activa;

	// sin save(): las personas estan gestionadas y el cambio se guarda al cerrar la transaccion
	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!activa) {
			return;
		}
		List<Persona> enClaro = repositorio.findAll().stream()
				.filter(persona -> !persona.tieneLaContraseniaEncriptada())
				.toList();
		enClaro.forEach(persona -> persona.encriptarContrasenia(encriptador));
		if (!enClaro.isEmpty()) {
			log.info("Se encriptaron las contrasenias de {} personas que todavia estaban en claro", enClaro.size());
		}
	}
}
