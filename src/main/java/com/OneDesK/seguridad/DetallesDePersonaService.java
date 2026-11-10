package com.OneDesK.seguridad;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.repositories.PersonaRepository;

/** De donde saca Spring Security a quien intenta entrar: la tabla Persona, buscando por email. */
@Service
public class DetallesDePersonaService implements UserDetailsService {

	@Autowired
	private PersonaRepository personaRepository;

	// transaccional porque los roles se leen de Persona_Rol al armar los detalles
	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		if (email == null) {
			throw new UsernameNotFoundException("Hace falta el email");
		}
		// Persona guarda el email en minusculas y sin espacios: se busca igual
		return personaRepository.findByEmail(email.trim().toLowerCase())
				.map(DetallesDePersona::new)
				.orElseThrow(() -> new UsernameNotFoundException("No hay ninguna cuenta con ese email"));
	}
}
