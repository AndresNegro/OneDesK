package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.excepciones.EmailDuplicadoException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.Usuario;
import com.OneDesK.repositories.PersonaRepository;
import com.OneDesK.repositories.UsuarioRepository;

@Service
public class UsuarioServiceImpl implements UsuarioService {

	@Autowired
	private UsuarioRepository repositorio;
	@Autowired
	private PersonaRepository personaRepository;

	@Override
	@Transactional
	public Usuario registrar(String nombre, String apellido, String email, String contrasenia) {
		Usuario usuario = new Usuario(nombre, apellido, email, contrasenia);

		// se consulta con el email que ya normalizo Persona, asi las mayusculas no esquivan el chequeo
		if (personaRepository.existsByEmail(usuario.getEmail())) {
			throw new EmailDuplicadoException("Ya existe una persona registrada con el email " + usuario.getEmail());
		}
		return repositorio.save(usuario);
	}

	@Override
	@Transactional
	public Usuario asignarTopeCredito(int usuarioId, int tope) {
		Usuario usuario = repositorio.findById(usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + usuarioId));
		usuario.setTopeCredito(tope);
		return repositorio.save(usuario);
	}
}
