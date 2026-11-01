package com.OneDesK.services;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.excepciones.EmailDuplicadoException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.dtos.DTO;
import com.OneDesK.modelo.Usuario;
import com.OneDesK.repositories.PersonaRepository;
import com.OneDesK.repositories.UsuarioRepository;

@Service
public class UsuarioServiceImpl extends ExponibleServiceImpl<Usuario> implements UsuarioService {

	/** Por la API se muestran los clientes aprobados, no las solicitudes pendientes. */
	@Override
	protected List<Usuario> aExponer() {
		return listarAprobados();
	}


	@Autowired
	private UsuarioRepository repositorio;
	@Autowired
	private PersonaRepository personaRepository;
	@Autowired
	private ModelMapper mapeador;

	@Override
	@Transactional
	public List<DTO> exponerParaAdmin() {
		List<Usuario> usuarios = repositorio.findAll();

		return usuarios.stream().map(usuario -> mapeador.map(usuario, usuario.dtoParaAdmin()))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public List<Usuario> listarAprobados() {
		return repositorio.findByAprobadoTrueOrderByApellidoAscNombreAsc();
	}

	@Override
	@Transactional
	public Usuario registrar(String nombre, String apellido, String email, String contrasenia) {
		Usuario usuario = new Usuario(nombre, apellido, email, contrasenia);

		// se consulta con el email que ya normalizo Persona, asi las mayusculas no esquivan el chequeo
		if (personaRepository.existsByEmail(usuario.getEmail())) {
			throw new EmailDuplicadoException("error.email.duplicado", usuario.getEmail());
		}
		return repositorio.save(usuario);
	}

	@Override
	@Transactional
	public Usuario asignarTopeCredito(int usuarioId, int tope) {
		Usuario usuario = buscar(usuarioId);

		// sin save: el usuario ya esta gestionado y el cambio se guarda solo al terminar la transaccion.
		// save() sobre algo ya guardado hace merge, y el merge rompe el borrado de compras que venga despues.
		usuario.setTopeCredito(tope);
		return usuario;
	}

	@Override
	@Transactional
	public Usuario buscar(int usuarioId) {
		return repositorio.findById(usuarioId)
				.orElseThrow(() -> new RecursoNoEncontradoException("error.no.existe.usuario", usuarioId));
	}
}
