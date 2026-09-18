package com.OneDesK.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.OneDesK.excepciones.OperacionInvalidaException;

public class PlantaTest {

	private static final LocalDate HOY = LocalDate.now();

	// Una planta con datos validos se crea y arranca sin cosechar
	@Test
	public void crearUnaPlantaValida() {
		Planta planta = new Planta("OG Kush", HOY.minusDays(10), HOY.minusDays(20), 60, 120, 30);

		assertEquals("OG Kush", planta.getGenetica());
		assertFalse(planta.isCosechada());
	}

	// Germinar y plantar el mismo dia es valido
	@Test
	public void sePuedeGerminarYPlantarElMismoDia() {
		Planta planta = new Planta("OG Kush", HOY, HOY, 60, 120, 30);

		assertEquals("OG Kush", planta.getGenetica());
	}

	// Una planta no puede figurar germinada despues de plantada, porque la semilla germina antes
	@Test
	public void noSePuedePlantarAntesDeGerminar() {
		assertThrows(IllegalArgumentException.class,
				() -> new Planta("OG Kush", HOY.minusDays(20), HOY.minusDays(10), 60, 120, 30));
	}

	// La fecha de plantado no puede ser futura
	@Test
	public void noSePuedePlantarEnElFuturo() {
		assertThrows(IllegalArgumentException.class,
				() -> new Planta("OG Kush", HOY.plusDays(1), HOY, 60, 120, 30));
	}

	// Las fechas de plantado y de germinado son obligatorias
	@Test
	public void lasFechasSonObligatorias() {
		assertThrows(IllegalArgumentException.class, () -> new Planta("OG Kush", null, HOY, 60, 120, 30));
		assertThrows(IllegalArgumentException.class, () -> new Planta("OG Kush", HOY, null, 60, 120, 30));
	}

	// No se puede crear una planta con la genetica vacia
	@Test
	public void laGeneticaNoPuedeEstarVacia() {
		assertThrows(IllegalArgumentException.class, () -> new Planta("  ", HOY, HOY, 60, 120, 30));
	}

	// Los tiempos de riego, luz y ventilacion tienen que ser mayores a 0
	@Test
	public void losTiemposTienenQueSerPositivos() {
		assertThrows(IllegalArgumentException.class, () -> new Planta("OG Kush", HOY, HOY, 0, 120, 30));
		assertThrows(IllegalArgumentException.class, () -> new Planta("OG Kush", HOY, HOY, 60, -1, 30));
		assertThrows(IllegalArgumentException.class, () -> new Planta("OG Kush", HOY, HOY, 60, 120, 0));
	}

	// Cosechar marca la planta como cosechada con la fecha de hoy
	@Test
	public void cosecharLaMarcaConLaFechaDeHoy() {
		Planta planta = new Planta("OG Kush", HOY.minusDays(10), HOY.minusDays(20), 60, 120, 30);

		planta.cosechar();

		assertTrue(planta.isCosechada());
		assertEquals(HOY, planta.getFechaCosecha());
	}

	// Cosechar una planta que ya fue cosechada se rechaza
	@Test
	public void noSePuedeCosecharDosVeces() {
		Planta planta = new Planta("OG Kush", HOY.minusDays(10), HOY.minusDays(20), 60, 120, 30);
		planta.cosechar();

		assertThrows(OperacionInvalidaException.class, planta::cosechar);
	}

	// Una planta que no esta en ningun indoor no tiene donde dejar eventos: la revision no genera ninguno
	@Test
	public void unaPlantaSinIndoorNoGeneraEventos() {
		Planta planta = new Planta("OG Kush", HOY.minusDays(10), HOY.minusDays(20), 60, 120, 30);

		assertEquals(0, planta.generarEventosVencidos(LocalDateTime.now()));
	}
}
