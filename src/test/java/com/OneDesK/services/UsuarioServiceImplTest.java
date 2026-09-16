package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.OneDesK.excepciones.EmailDuplicadoException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.Usuario;
import com.OneDesK.repositories.PersonaRepository;
import com.OneDesK.repositories.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
public class UsuarioServiceImplTest {

	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private PersonaRepository personaRepository;

	@InjectMocks
	private UsuarioServiceImpl service;

	// --- registrar ---

	// Registrar un usuario con un email libre lo guarda con el repositorio
	@Test
	public void registrarGuardaAlUsuario() {
		when(personaRepository.existsByEmail("andres@test.com")).thenReturn(false);
		guardaElUsuario();

		Usuario usuario = service.registrar("Andres", "Negro", "andres@test.com", "12345");

		assertEquals("andres@test.com", usuario.getEmail());
		verify(usuarioRepository).save(usuario);
	}

	// Si el email ya existe se rechaza el registro y no se guarda nada
	@Test
	public void noSePuedeRegistrarUnEmailRepetido() {
		when(personaRepository.existsByEmail("andres@test.com")).thenReturn(true);

		assertThrows(EmailDuplicadoException.class,
				() -> service.registrar("Andres", "Negro", "andres@test.com", "12345"));

		verify(usuarioRepository, never()).save(any());
	}

	// La consulta de email repetido usa el email normalizado, asi escribirlo en mayusculas no la esquiva
	@Test
	public void elEmailRepetidoSeDetectaAunqueCambienLasMayusculas() {
		when(personaRepository.existsByEmail("andres@test.com")).thenReturn(true);

		assertThrows(EmailDuplicadoException.class,
				() -> service.registrar("Andres", "Negro", "ANDRES@Test.com", "12345"));
	}

	// Un email invalido falla al crear el usuario, antes de usar cualquier repositorio
	@Test
	public void unEmailInvalidoNiSiquieraConsultaLaBase() {
		assertThrows(IllegalArgumentException.class, () -> service.registrar("Andres", "Negro", "@", "12345"));

		verifyNoInteractions(personaRepository, usuarioRepository);
	}

	// --- asignarTopeCredito ---

	// Asignar un tope de credito lo actualiza en el usuario
	@Test
	public void asignarTopeCreditoLoActualiza() {
		Usuario usuario = new Usuario("Andres", "Negro", "andres@test.com", "12345");
		when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

		service.asignarTopeCredito(1, 8000);

		assertEquals(8000, usuario.getTopeCredito());
	}

	// Asignar tope a un usuario que no existe falla con RecursoNoEncontradoException
	@Test
	public void asignarTopeAUnUsuarioInexistenteFalla() {
		when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

		assertThrows(RecursoNoEncontradoException.class, () -> service.asignarTopeCredito(99, 8000));
	}

	// Un tope negativo se rechaza y el usuario no llega a guardarse
	@Test
	public void unTopeNegativoSeRechazaYNoSeGuarda() {
		Usuario usuario = new Usuario("Andres", "Negro", "andres@test.com", "12345");
		when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

		assertThrows(IllegalArgumentException.class, () -> service.asignarTopeCredito(1, -500));

		verify(usuarioRepository, never()).save(any());
	}

	private void guardaElUsuario() {
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
	}
}
