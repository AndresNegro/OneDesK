package com.OneDesK.web.validadores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.validation.BeanPropertyBindingResult;

import com.OneDesK.DatosDePrueba;
import com.OneDesK.modelo.Usuario;
import com.OneDesK.web.formularios.FormularioDeRegistro;

// Este validador consulta la base para saber si el email ya esta tomado, asi que se prueba contra
// MySQL con el repositorio de verdad, igual que los services.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ValidadorDeRegistro.class)
public class ValidadorDeRegistroTest {

	@Autowired
	private ValidadorDeRegistro validador;
	@Autowired
	private TestEntityManager em;

	// Una solicitud bien completada, con un email libre, pasa entera
	@Test
	public void unaSolicitudCompletaPasa() {
		assertFalse(revisar(solicitudCompleta()).hasErrors());
	}

	// Si las dos contrasenias no coinciden, el rechazo cae en el campo de repetirla
	@Test
	public void lasDosContraseniasTienenQueCoincidir() {
		FormularioDeRegistro form = solicitudCompleta();
		form.setRepetir("otra-cosa");

		assertEquals("error.contrasenias.distintas", claveDe(revisar(form), "repetir"));
	}

	// Una contrasenia de menos de 5 caracteres se rechaza, y el mensaje recibe ese minimo
	@Test
	public void laContraseniaTieneUnMinimo() {
		FormularioDeRegistro form = solicitudCompleta();
		form.setContrasenia("123");
		form.setRepetir("123");

		BeanPropertyBindingResult errores = revisar(form);

		assertEquals("validacion.contrasenia.corta", claveDe(errores, "contrasenia"));
		assertEquals(5, errores.getFieldError("contrasenia").getArguments()[0]);
	}

	// Los campos vacios se rechazan cada uno con su clave
	@Test
	public void losDatosVienenCompletos() {
		BeanPropertyBindingResult errores = revisar(new FormularioDeRegistro());

		assertEquals("validacion.nombre.vacio", claveDe(errores, "nombre"));
		assertEquals("validacion.apellido.vacio", claveDe(errores, "apellido"));
		assertEquals("validacion.email.vacio", claveDe(errores, "email"));
		assertEquals("validacion.contrasenia.vacia", claveDe(errores, "contrasenia"));
	}

	// Si ya hay una persona con ese email, el aviso sale abajo del campo antes de intentar guardar
	@Test
	public void noSePuedePedirUnaCuentaConUnEmailTomado() {
		Usuario usuario = new Usuario("Andrés", "Negro", DatosDePrueba.email(), "12345");
		em.persistAndFlush(usuario);
		FormularioDeRegistro form = solicitudCompleta();
		form.setEmail(usuario.getEmail().toUpperCase());

		assertEquals("validacion.email.tomado", claveDe(revisar(form), "email"));
	}

	// Con el email vacio no se va a buscar a la base: el motivo es que falta, no que este tomado
	@Test
	public void elEmailVacioNoSeBuscaEnLaBase() {
		FormularioDeRegistro form = solicitudCompleta();
		form.setEmail("   ");

		assertEquals("validacion.email.vacio", claveDe(revisar(form), "email"));
	}

	private FormularioDeRegistro solicitudCompleta() {
		FormularioDeRegistro form = new FormularioDeRegistro();
		form.setNombre("Juan");
		form.setApellido("Perez");
		form.setEmail(DatosDePrueba.email());
		form.setContrasenia("12345");
		form.setRepetir("12345");
		return form;
	}

	private BeanPropertyBindingResult revisar(FormularioDeRegistro form) {
		BeanPropertyBindingResult errores = new BeanPropertyBindingResult(form, "form");
		validador.validate(form, errores);
		return errores;
	}

	private String claveDe(BeanPropertyBindingResult errores, String campo) {
		return errores.getFieldError(campo) == null ? null : errores.getFieldError(campo).getCode();
	}
}
