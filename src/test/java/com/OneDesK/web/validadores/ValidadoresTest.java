package com.OneDesK.web.validadores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import com.OneDesK.web.formularios.FormularioDeIngreso;
import com.OneDesK.web.formularios.FormularioDeLimites;
import com.OneDesK.web.formularios.FormularioDePlanta;

// Los validadores se prueban solos, sin levantar Spring: se les pasa un formulario y se mira
// que campo rechazaron y con que clave, que es la que despues busca el texto en messages.properties.
public class ValidadoresTest {

	// Un login sin email ni contrasenia rechaza los dos campos, cada uno con su clave
	@Test
	public void elLoginPideLosDosCampos() {
		FormularioDeIngreso form = new FormularioDeIngreso();
		form.setEmail("   ");

		Errores errores = revisar(new ValidadorDeIngreso(), form);

		assertEquals("validacion.email.vacio", errores.claveDe("email"));
		assertEquals("validacion.contrasenia.vacia", errores.claveDe("contrasenia"));
	}

	// Con los dos campos completos no hay nada que rechazar
	@Test
	public void unLoginCompletoPasa() {
		FormularioDeIngreso form = new FormularioDeIngreso();
		form.setEmail("andres@test.com");
		form.setContrasenia("12345");

		assertFalse(revisar(new ValidadorDeIngreso(), form).hayErrores());
	}

	// No se puede plantar con fecha futura ni germinar despues de plantada
	@Test
	public void lasFechasDeLaPlantaVanEnOrden() {
		FormularioDePlanta form = new FormularioDePlanta();
		form.setGenetica("OG Kush");
		form.setFechaPlantado(LocalDate.now().plusDays(1));
		form.setFechaGerminado(LocalDate.now().plusDays(2));
		form.setTiempoRegado(60);
		form.setTiempoLuz(60);
		form.setTiempoVentilacion(60);

		Errores errores = revisar(new ValidadorDePlanta(), form);

		assertEquals("validacion.plantado.futuro", errores.claveDe("fechaPlantado"));
		assertEquals("validacion.germinado.tarde", errores.claveDe("fechaGerminado"));
	}

	// Los tres tiempos tienen que ser de al menos un minuto
	@Test
	public void losTiemposDeLaPlantaSonPositivos() {
		FormularioDePlanta form = new FormularioDePlanta();
		form.setGenetica("OG Kush");
		form.setFechaPlantado(LocalDate.now());
		form.setFechaGerminado(LocalDate.now().minusDays(10));

		Errores errores = revisar(new ValidadorDePlanta(), form);

		assertEquals("validacion.tiempo", errores.claveDe("tiempoRegado"));
		assertEquals("validacion.tiempo", errores.claveDe("tiempoLuz"));
		assertEquals("validacion.tiempo", errores.claveDe("tiempoVentilacion"));
	}

	// El maximo de gramos no puede quedar por debajo del minimo
	@Test
	public void elMaximoNoPuedeSerMenorQueElMinimo() {
		FormularioDeLimites form = new FormularioDeLimites();
		form.setMinimo(10);
		form.setMaximo(5);

		assertEquals("validacion.maximo", revisar(new ValidadorDeLimites(), form).claveDe("maximo"));
	}

	// Cada validador dice que formulario sabe revisar: el de limites no se mete con el de ingreso
	@Test
	public void cadaValidadorSabeQueFormularioRevisa() {
		assertTrue(new ValidadorDeLimites().supports(FormularioDeLimites.class));
		assertFalse(new ValidadorDeLimites().supports(FormularioDeIngreso.class));
	}

	// A un validador le puede llegar un formulario que no es el suyo, porque el controller
	// engancha varios juntos: en ese caso no toca nada
	@Test
	public void unValidadorIgnoraLosFormulariosQueNoSonSuyos() {
		FormularioDeLimites limites = new FormularioDeLimites();

		assertFalse(revisar(new ValidadorDeIngreso(), limites).hayErrores());
	}

	private Errores revisar(ValidadorDeFormulario<?> validador, Object form) {
		Errors resultado = new BeanPropertyBindingResult(form, "form");
		validador.validate(form, resultado);
		return new Errores((BeanPropertyBindingResult) resultado);
	}

	/** Lo que quedo rechazado, para poder preguntarlo campo por campo. */
	private static class Errores {

		private final BeanPropertyBindingResult resultado;

		Errores(BeanPropertyBindingResult resultado) {
			this.resultado = resultado;
		}

		boolean hayErrores() {
			return resultado.hasErrors();
		}

		String claveDe(String campo) {
			return resultado.getFieldError(campo) == null ? null : resultado.getFieldError(campo).getCode();
		}
	}
}
