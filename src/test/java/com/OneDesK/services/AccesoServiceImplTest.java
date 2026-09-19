package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.OneDesK.excepciones.CredencialesInvalidasException;
import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.modelo.Administrador;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Persona;
import com.OneDesK.modelo.Usuario;

// El login contra MySQL: las tres clases de persona se buscan por email en la misma tabla Persona
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AccesoServiceImpl.class)
public class AccesoServiceImplTest {

	@Autowired
	private AccesoService service;
	@Autowired
	private TestEntityManager em;

	@BeforeEach
	public void setUp() {
		Usuario aprobado = new Usuario("Juan", "Perez", "usuario@test.com", "clave-usuario");
		aprobado.aprobar(0);
		em.persist(aprobado);
		em.persist(new Usuario("Pepe", "Pendiente", "pendiente@test.com", "clave-pendiente"));
		em.persist(new EmpleadoIndoor("Pedro", "Gomez", "empleado@test.com", "clave-empleado", 500000));
		em.persist(new Administrador("Ana", "Admin", "admin@test.com", "clave-admin"));
		em.flush();
		em.clear();
	}

	// Con email y contrasenia correctos devuelve la persona, con su clase real
	@Test
	public void ingresarDevuelveLaPersonaConSuClase() {
		assertInstanceOf(Usuario.class, service.ingresar("usuario@test.com", "clave-usuario"));
		assertInstanceOf(EmpleadoIndoor.class, service.ingresar("empleado@test.com", "clave-empleado"));
		assertInstanceOf(Administrador.class, service.ingresar("admin@test.com", "clave-admin"));
	}

	// El email se busca normalizado: mayusculas y espacios no impiden entrar
	@Test
	public void elEmailSeBuscaSinImportarMayusculasNiEspacios() {
		Persona persona = service.ingresar("  USUARIO@Test.com ", "clave-usuario");

		assertEquals("usuario@test.com", persona.getEmail());
	}

	// La contrasenia si distingue mayusculas
	@Test
	public void laContraseniaDistingueMayusculas() {
		assertThrows(CredencialesInvalidasException.class, () -> service.ingresar("usuario@test.com", "CLAVE-USUARIO"));
	}

	// Una contrasenia equivocada y un email inexistente dan el mismo error, sin decir cual fallo
	@Test
	public void emailInexistenteYContraseniaEquivocadaDanElMismoError() {
		String inexistente = assertThrows(CredencialesInvalidasException.class,
				() -> service.ingresar("nadie@test.com", "clave-usuario")).getMessage();
		String equivocada = assertThrows(CredencialesInvalidasException.class,
				() -> service.ingresar("usuario@test.com", "otra")).getMessage();

		assertEquals(inexistente, equivocada);
	}

	// Un usuario que todavia no fue aprobado no puede ingresar, y se le dice por que
	@Test
	public void unUsuarioPendienteNoPuedeIngresar() {
		String mensaje = assertThrows(OperacionInvalidaException.class,
				() -> service.ingresar("pendiente@test.com", "clave-pendiente")).getMessage();

		assertTrue(mensaje.contains("esperando"));
	}

	// Con la contrasenia equivocada no se revela que la cuenta esta pendiente
	@Test
	public void conLaContraseniaEquivocadaNoSeRevelaQueEstaPendiente() {
		assertThrows(CredencialesInvalidasException.class, () -> service.ingresar("pendiente@test.com", "otra"));
	}

	// Sin email o sin contrasenia se rechaza sin tirar NullPointerException
	@Test
	public void datosVaciosSeRechazan() {
		assertThrows(CredencialesInvalidasException.class, () -> service.ingresar(null, "clave-usuario"));
		assertThrows(CredencialesInvalidasException.class, () -> service.ingresar("usuario@test.com", null));
	}
}
