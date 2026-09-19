package com.OneDesK.modelo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.OneDesK.excepciones.OperacionInvalidaException;

public class LimitesDeCompraTest {

	// Los limites arrancan en 5 g de minimo y 40 g de maximo
	@Test
	public void arrancanEnCincoYCuarentaGramos() {
		LimitesDeCompra limites = new LimitesDeCompra();

		assertEquals(5, limites.getMinimoGramos());
		assertEquals(40, limites.getMaximoGramos());
	}

	// Justo en el minimo y justo en el maximo se aceptan
	@Test
	public void losBordesSeAceptan() {
		LimitesDeCompra limites = new LimitesDeCompra();

		assertDoesNotThrow(() -> limites.verificar(5));
		assertDoesNotThrow(() -> limites.verificar(40));
	}

	// Un gramo menos que el minimo o uno mas que el maximo se rechaza, y el mensaje dice los limites
	@Test
	public void fueraDeLosLimitesSeRechaza() {
		LimitesDeCompra limites = new LimitesDeCompra();

		assertThrows(OperacionInvalidaException.class, () -> limites.verificar(4));
		String mensaje = assertThrows(OperacionInvalidaException.class, () -> limites.verificar(41)).getMessage();
		assertTrue(mensaje.contains("5 g") && mensaje.contains("40 g"));
	}

	// Cambiarlos aplica los valores nuevos a la verificacion
	@Test
	public void cambiarlosAplicaLosNuevosValores() {
		LimitesDeCompra limites = new LimitesDeCompra();

		limites.cambiar(10, 20);

		assertThrows(OperacionInvalidaException.class, () -> limites.verificar(9));
		assertDoesNotThrow(() -> limites.verificar(20));
		assertThrows(OperacionInvalidaException.class, () -> limites.verificar(21));
	}

	// El minimo tiene que ser al menos 1 g y el maximo no puede ser menor que el minimo; si no, no cambia nada
	@Test
	public void valoresInvalidosNoCambianNada() {
		LimitesDeCompra limites = new LimitesDeCompra();

		assertThrows(IllegalArgumentException.class, () -> limites.cambiar(0, 40));
		assertThrows(IllegalArgumentException.class, () -> limites.cambiar(30, 20));

		assertEquals(5, limites.getMinimoGramos());
		assertEquals(40, limites.getMaximoGramos());
	}

	// Minimo y maximo iguales se permite: todas las compras de la misma cantidad
	@Test
	public void minimoYMaximoIgualesSePermite() {
		LimitesDeCompra limites = new LimitesDeCompra();

		limites.cambiar(10, 10);

		assertDoesNotThrow(() -> limites.verificar(10));
	}
}
