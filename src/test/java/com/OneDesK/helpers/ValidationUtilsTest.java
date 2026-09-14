package com.OneDesK.helpers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ValidationUtilsTest {

	@Test
	public void aceptaUnEmailComun() {
		assertTrue(ValidationUtils.isValidEmail("andres@test.com"));
	}

	@Test
	public void aceptaUnDominioConVariosPuntos() {
		assertTrue(ValidationUtils.isValidEmail("andres@mail.test.com"));
	}

	@Test
	public void aceptaEspaciosAlrededor() {
		assertTrue(ValidationUtils.isValidEmail("  andres@test.com  "));
	}

	@ParameterizedTest
	@ValueSource(strings = { "@", "andres", "andres@", "@test.com", "andres@test", "andres@test.", "andres@.com",
			"and res@test.com", "andres@@test.com", "an@dres@test.com", "", "   " })
	public void rechazaEmailsMalFormados(String email) {
		assertFalse(ValidationUtils.isValidEmail(email));
	}

	@Test
	public void rechazaUnEmailNulo() {
		assertFalse(ValidationUtils.isValidEmail(null));
	}

	@Test
	public void tieneMasDeAceptaExactamenteElMinimo() {
		assertTrue(ValidationUtils.tieneMasDe("12345", 5));
		assertFalse(ValidationUtils.tieneMasDe("1234", 5));
		assertFalse(ValidationUtils.tieneMasDe(null, 5));
	}

	@Test
	public void tieneTextoRechazaVaciosYEspacios() {
		assertTrue(ValidationUtils.tieneTexto("Andres"));
		assertFalse(ValidationUtils.tieneTexto(""));
		assertFalse(ValidationUtils.tieneTexto("   "));
		assertFalse(ValidationUtils.tieneTexto(null));
	}
}
