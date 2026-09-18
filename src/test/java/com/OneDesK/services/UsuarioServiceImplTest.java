package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.OneDesK.excepciones.EmailDuplicadoException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.Usuario;

// Contra MySQL con el service real: cada resultado se comprueba leyendo la base, no el objeto en memoria
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UsuarioServiceImpl.class)
public class UsuarioServiceImplTest {

	@Autowired
	private UsuarioService service;
	@Autowired
	private TestEntityManager em;

	// --- registrar ---

	// Registrar un usuario con un email libre lo deja guardado en la base
	@Test
	public void registrarGuardaAlUsuario() {
		int id = service.registrar("Andres", "Negro", "andres@test.com", "12345").getId();
		em.flush();
		em.clear();

		Usuario recargado = em.find(Usuario.class, id);
		assertEquals("andres@test.com", recargado.getEmail());
		assertEquals(0, recargado.getTopeCredito());
		assertEquals(0, recargado.getDeuda().getMonto());
	}

	// Si el email ya esta registrado se rechaza y en la base sigue habiendo una sola persona con ese email
	@Test
	public void noSePuedeRegistrarUnEmailRepetido() {
		service.registrar("Andres", "Negro", "andres@test.com", "12345");
		em.flush();

		assertThrows(EmailDuplicadoException.class,
				() -> service.registrar("Otro", "Usuario", "andres@test.com", "12345"));

		assertEquals(1, personasConEmail("andres@test.com"));
	}

	// El email se compara normalizado, asi escribirlo en mayusculas no esquiva el chequeo
	@Test
	public void elEmailRepetidoSeDetectaAunqueCambienLasMayusculas() {
		service.registrar("Andres", "Negro", "andres@test.com", "12345");
		em.flush();

		assertThrows(EmailDuplicadoException.class,
				() -> service.registrar("Andres", "Negro", "ANDRES@Test.com", "12345"));

		assertEquals(1, personasConEmail("andres@test.com"));
	}

	// Un email invalido falla al crear el usuario y no llega nada a la base
	@Test
	public void unEmailInvalidoNoGuardaNada() {
		long antes = contarPersonas();

		assertThrows(IllegalArgumentException.class, () -> service.registrar("Andres", "Negro", "@", "12345"));
		em.flush();

		assertEquals(antes, contarPersonas());
	}

	// --- asignarTopeCredito ---

	// Asignar un tope de credito lo deja guardado en la base, sin necesidad de save
	@Test
	public void asignarTopeCreditoQuedaGuardado() {
		int id = service.registrar("Andres", "Negro", "andres@test.com", "12345").getId();
		em.flush();
		em.clear();

		service.asignarTopeCredito(id, 8000);
		em.flush();
		em.clear();

		assertEquals(8000, em.find(Usuario.class, id).getTopeCredito());
	}

	// Asignar tope a un usuario que no existe falla con RecursoNoEncontradoException
	@Test
	public void asignarTopeAUnUsuarioInexistenteFalla() {
		assertThrows(RecursoNoEncontradoException.class, () -> service.asignarTopeCredito(999999, 8000));
	}

	// Un tope negativo se rechaza y en la base queda el tope anterior
	@Test
	public void unTopeNegativoSeRechazaYQuedaElAnterior() {
		int id = service.registrar("Andres", "Negro", "andres@test.com", "12345").getId();
		service.asignarTopeCredito(id, 5000);
		em.flush();
		em.clear();

		assertThrows(IllegalArgumentException.class, () -> service.asignarTopeCredito(id, -500));
		em.flush();
		em.clear();

		assertEquals(5000, em.find(Usuario.class, id).getTopeCredito());
	}

	private long personasConEmail(String email) {
		return em.getEntityManager()
				.createQuery("select count(p) from Persona p where p.email = :email", Long.class)
				.setParameter("email", email).getSingleResult();
	}

	private long contarPersonas() {
		return em.getEntityManager().createQuery("select count(p) from Persona p", Long.class).getSingleResult();
	}
}
