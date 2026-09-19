package com.OneDesK.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.OneDesK.repositories.AdministradorRepository;
import com.OneDesK.services.AdministradorService;

/**
 * El primer administrador no tiene pantalla de registro: si al arrancar no hay ninguno, se crea con el
 * email y la contrasenia de onedesk.admin.email y onedesk.admin.contrasenia. Esos datos van en
 * db-local.properties, que no se sube a GitHub. Si faltan, no se crea nada y se avisa en el log.
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

	@Autowired
	private AdministradorRepository repositorio;
	@Autowired
	private AdministradorService administradorService;

	@Value("${onedesk.admin.email:}")
	private String email;
	@Value("${onedesk.admin.contrasenia:}")
	private String contrasenia;

	@Override
	public void run(ApplicationArguments args) {
		if (repositorio.count() > 0) {
			return;
		}
		if (email.isBlank() || contrasenia.isBlank()) {
			log.warn("No hay ningun administrador y faltan onedesk.admin.email y onedesk.admin.contrasenia"
					+ " en db-local.properties: no se pudo crear el primero");
			return;
		}
		administradorService.registrar("Administrador", "OneDesK", email, contrasenia);
		log.info("Se creo el primer administrador con el email {}", email);
	}
}
