package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.OneDesK.DatosDePrueba;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;

/**
 * Los avisos por mail, contra un servidor de correo de verdad levantado en memoria: la aplicacion
 * manda el mail como lo mandaria en produccion y el test lee el que llego.
 */
@SpringBootTest(properties = { "onedesk.eventos.activo=false", "onedesk.mail.activo=true",
		"spring.mail.host=localhost", "spring.mail.port=3025" })
public class AvisosPorCorreoTest {

	private static final String CLAVE = "12345";

	@Autowired
	private AdministradorService administradorService;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private JdbcTemplate jdbc;

	private GreenMail servidorDeCorreo;
	private int admin;
	private String email;

	@BeforeEach
	public void levantarElCorreo() {
		servidorDeCorreo = new GreenMail(new ServerSetup(3025, null, ServerSetup.PROTOCOL_SMTP));
		servidorDeCorreo.start();
		admin = administradorService.registrar("Ana", "Admin", DatosDePrueba.email(), CLAVE).getId();
		email = DatosDePrueba.email();
	}

	@AfterEach
	public void bajarElCorreoYBorrarLasPersonas() {
		servidorDeCorreo.stop();
		jdbc.update("DELETE u, p, d FROM Usuario u JOIN Persona p ON p.ID = u.ID"
				+ " LEFT JOIN Deuda d ON d.ID = u.ID_DEUDA WHERE p.email LIKE 'prueba-%@test.com'");
		jdbc.update("DELETE a, p FROM Administrador a JOIN Persona p ON p.ID = a.ID WHERE p.email LIKE 'prueba-%@test.com'");
	}

	// Al aprobar la cuenta le llega el mail, con su nombre y el tope que le pusieron
	@Test
	public void alAprobarLaCuentaLlegaElMail() throws Exception {
		int usuario = usuarioService.registrar("Juan", "Perez", email, CLAVE).getId();

		administradorService.aprobarUsuario(admin, usuario, 5000);

		MimeMessage mail = esperarUnMail();
		assertEquals(email, mail.getAllRecipients()[0].toString());
		assertEquals("Tu cuenta de OneDesK ya está habilitada", mail.getSubject());
		String cuerpo = GreenMailUtil.getBody(mail);
		assertTrue(cuerpo.contains("Juan Perez"), "El mail no saluda por el nombre: " + cuerpo);
		assertTrue(cuerpo.contains("5000"), "El mail no dice el tope de credito: " + cuerpo);
	}

	// Al rechazar la solicitud tambien se avisa, aunque esa persona ya no este en la base
	@Test
	public void alRechazarLaSolicitudLlegaElMail() throws Exception {
		int usuario = usuarioService.registrar("Pepe", "Pendiente", email, CLAVE).getId();

		administradorService.rechazarUsuario(admin, usuario);

		MimeMessage mail = esperarUnMail();
		assertEquals(email, mail.getAllRecipients()[0].toString());
		assertEquals("Sobre tu solicitud en OneDesK", mail.getSubject());
		assertTrue(GreenMailUtil.getBody(mail).contains("Pepe Pendiente"));
		assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM Persona WHERE email = ?", Integer.class, email));
	}

	// Registrarse no manda ningun mail: el aviso sale recien cuando la administracion resuelve
	@Test
	public void registrarseTodaviaNoMandaNada() throws Exception {
		usuarioService.registrar("Juan", "Perez", email, CLAVE);

		assertEquals(0, servidorDeCorreo.getReceivedMessages().length);
	}

	// Si la operacion falla no sale ningun mail: el aviso se manda recien cuando quedo guardada
	@Test
	public void siLaOperacionFallaNoSeAvisa() throws Exception {
		int usuario = usuarioService.registrar("Juan", "Perez", email, CLAVE).getId();
		administradorService.aprobarUsuario(admin, usuario, 5000);
		esperarUnMail();

		// rechazar una cuenta ya aprobada no se puede: la transaccion se deshace
		try {
			administradorService.rechazarUsuario(admin, usuario);
		} catch (RuntimeException esperado) {
			// el motivo lo prueba AdministradorServiceImplTest: aca importa que no salga el mail
		}

		assertEquals(1, servidorDeCorreo.getReceivedMessages().length);
	}

	private MimeMessage esperarUnMail() {
		assertTrue(servidorDeCorreo.waitForIncomingEmail(5000, 1), "No llego ningun mail");
		return servidorDeCorreo.getReceivedMessages()[0];
	}
}
