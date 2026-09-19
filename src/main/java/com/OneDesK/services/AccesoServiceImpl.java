package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.excepciones.CredencialesInvalidasException;
import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.modelo.Persona;
import com.OneDesK.modelo.Usuario;
import com.OneDesK.repositories.PersonaRepository;

@Service
public class AccesoServiceImpl implements AccesoService {

	@Autowired
	private PersonaRepository personaRepository;

	@Override
	@Transactional
	public Persona ingresar(String email, String contrasenia) {
		if (email == null || contrasenia == null) {
			throw new CredencialesInvalidasException();
		}
		// Persona guarda el email en minusculas y sin espacios: se busca igual
		Persona persona = personaRepository.findByEmail(email.trim().toLowerCase())
				.orElseThrow(CredencialesInvalidasException::new);
		if (!persona.getContrasenia().equals(contrasenia)) {
			throw new CredencialesInvalidasException();
		}
		// se avisa despues de validar la contrasenia: solo el duenio de la cuenta sabe que esta pendiente
		if (persona instanceof Usuario && !((Usuario) persona).isAprobado()) {
			throw new OperacionInvalidaException(
					"Tu cuenta todavía está esperando que la administración la apruebe");
		}
		return persona;
	}
}
