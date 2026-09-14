package com.OneDesK.helpers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ValidationUtilsTest {

	// Un email con formato usuario@dominio.com es valido
	@Test
	public void aceptaUnEmailComun() {
		assertTrue(ValidationUtils.isValidEmail("andres@test.com"));
	}

	// Un dominio con subdominios, como mail.test.com, tambien es valido
	@Test
	public void aceptaUnDominioConVariosPuntos() {
		assertTrue(ValidationUtils.isValidEmail("andres@mail.test.com"));
	}

	// Los espacios al principio y al final no invalidan el email
	@Test
	public void aceptaEspaciosAlrededor() {
		assertTrue(ValidationUtils.isValidEmail("  andres@test.com  "));
	}

	// Cada email de la lista tiene un defecto de formato distinto y todos tienen que rechazarse
	@ParameterizedTest
	@ValueSource(strings = { "@", "andres", "andres@", "@test.com", "andres@test", "andres@test.", "andres@.com",
			"and res@test.com", "andres@@test.com", "an@dres@test.com", "", "   " })
	public void rechazaEmailsMalFormados(String email) {
		assertFalse(ValidationUtils.isValidEmail(email));
	}

	// Un email nulo se rechaza sin tirar NullPointerException
	@Test
	public void rechazaUnEmailNulo() {
		assertFalse(ValidationUtils.isValidEmail(null));
	}

	// tieneMasDe acepta un texto con justo la cantidad minima y rechaza uno mas corto o nulo
	@Test
	public void tieneMasDeAceptaExactamenteElMinimo() {
		assertTrue(ValidationUtils.tieneMasDe("12345", 5));
		assertFalse(ValidationUtils.tieneMasDe("1234", 5));
		assertFalse(ValidationUtils.tieneMasDe(null, 5));
	}

	// tieneTexto solo acepta textos con al menos un caracter que no sea espacio
	@Test
	public void tieneTextoRechazaVaciosYEspacios() {
		assertTrue(ValidationUtils.tieneTexto("Andres"));
		assertFalse(ValidationUtils.tieneTexto(""));
		assertFalse(ValidationUtils.tieneTexto("   "));
		assertFalse(ValidationUtils.tieneTexto(null));
	}
}
