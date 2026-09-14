package com.OneDesK.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.OneDesK.excepciones.OperacionInvalidaException;

public class PlantaTest {

	private static final LocalDate HOY = LocalDate.now();

	@Test
	public void crearUnaPlantaValida() {
		Planta planta = new Planta("OG Kush", HOY.minusDays(10), HOY.minusDays(20), 60, 120, 30);

		assertEquals("OG Kush", planta.getGenetica());
		assertFalse(planta.isCosechada());
	}

	@Test
	public void sePuedeGerminarYPlantarElMismoDia() {
		Planta planta = new Planta("OG Kush", HOY, HOY, 60, 120, 30);

		assertEquals("OG Kush", planta.getGenetica());
	}

	@Test
	public void noSePuedePlantarAntesDeGerminar() {
		assertThrows(IllegalArgumentException.class,
				() -> new Planta("OG Kush", HOY.minusDays(20), HOY.minusDays(10), 60, 120, 30));
	}

	@Test
	public void noSePuedePlantarEnElFuturo() {
		assertThrows(IllegalArgumentException.class,
				() -> new Planta("OG Kush", HOY.plusDays(1), HOY, 60, 120, 30));
	}

	@Test
	public void lasFechasSonObligatorias() {
		assertThrows(IllegalArgumentException.class, () -> new Planta("OG Kush", null, HOY, 60, 120, 30));
		assertThrows(IllegalArgumentException.class, () -> new Planta("OG Kush", HOY, null, 60, 120, 30));
	}

	@Test
	public void laGeneticaNoPuedeEstarVacia() {
		assertThrows(IllegalArgumentException.class, () -> new Planta("  ", HOY, HOY, 60, 120, 30));
	}

	@Test
	public void losTiemposTienenQueSerPositivos() {
		assertThrows(IllegalArgumentException.class, () -> new Planta("OG Kush", HOY, HOY, 0, 120, 30));
		assertThrows(IllegalArgumentException.class, () -> new Planta("OG Kush", HOY, HOY, 60, -1, 30));
		assertThrows(IllegalArgumentException.class, () -> new Planta("OG Kush", HOY, HOY, 60, 120, 0));
	}

	@Test
	public void cosecharLaMarcaConLaFechaDeHoy() {
		Planta planta = new Planta("OG Kush", HOY.minusDays(10), HOY.minusDays(20), 60, 120, 30);

		planta.cosechar();

		assertTrue(planta.isCosechada());
		assertEquals(HOY, planta.getFechaCosecha());
	}

	@Test
	public void noSePuedeCosecharDosVeces() {
		Planta planta = new Planta("OG Kush", HOY.minusDays(10), HOY.minusDays(20), 60, 120, 30);
		planta.cosechar();

		assertThrows(OperacionInvalidaException.class, planta::cosechar);
	}
}
