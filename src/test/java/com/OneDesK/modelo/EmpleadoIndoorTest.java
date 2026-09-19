package com.OneDesK.modelo;

import com.OneDesK.DatosDePrueba;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.OneDesK.evento.EventoLuz;
import com.OneDesK.evento.EventoRegado;
import com.OneDesK.evento.EventoVentilador;
import com.OneDesK.excepciones.OperacionInvalidaException;

public class EmpleadoIndoorTest {

	private EmpleadoIndoor empleado;
	private Indoor indoor;
	private Planta planta;

	@BeforeEach
	public void setUp() {
		empleado = new EmpleadoIndoor("Andres", "Negro", "empleado@test.com", "12345", 500000);
		indoor = DatosDePrueba.indoor();
		planta = indoor.addPlanta(nuevaPlanta("OG Kush"));
		empleado.addIndoor(indoor);
	}

	// --- salario ---

	// No se puede crear un empleado con salario 0
	@Test
	public void elSalarioTieneQueSerMayorACero() {
		assertThrows(IllegalArgumentException.class,
				() -> new EmpleadoIndoor("Andres", "Negro", "otro@test.com", "12345", 0));
	}

	// setSalarioMensual rechaza un salario negativo y conserva el anterior
	@Test
	public void cambiarElSalarioTambienSeValida() {
		assertThrows(IllegalArgumentException.class, () -> empleado.setSalarioMensual(-1));

		assertEquals(500000, empleado.getSalarioMensual());
	}

	// --- asignaciones ---

	// Asignar un indoor que el empleado ya tiene se rechaza
	@Test
	public void noSePuedeAsignarDosVecesElMismoIndoor() {
		assertThrows(OperacionInvalidaException.class, () -> empleado.addIndoor(indoor));

		assertEquals(1, empleado.getSectoresACargo().size());
	}

	// Desasignar un indoor lo saca de los indoors a cargo del empleado
	@Test
	public void desasignarLoSacaDeSusIndoors() {
		empleado.deleteIndoor(indoor);

		assertFalse(empleado.estaAsignadoA(indoor));
	}

	// Desasignar un indoor que el empleado no tiene se rechaza
	@Test
	public void noSePuedeDesasignarUnIndoorQueNoTiene() {
		assertThrows(OperacionInvalidaException.class, () -> empleado.deleteIndoor(DatosDePrueba.indoor()));
	}

	// --- eventos ---

	// Atender un riego lo marca como realizado y lo deja en el historial del indoor
	@Test
	public void atenderUnRiegoLoDejaComoRealizadoSinBorrarlo() {
		EventoRegado riego = new EventoRegado(planta);
		indoor.recibirEvento(riego);

		empleado.atenderEvento(riego);

		assertTrue(riego.getRealizado());
		assertEquals(0, indoor.getEventosPendientes().size());
		assertEquals(1, indoor.getColaEventos().size());
	}

	// Cada evento de luz atendido invierte la luz: el primero la prende y el segundo la apaga
	@Test
	public void cadaEventoDeLuzCambiaElEstadoDeLaLuz() {
		EventoLuz prender = new EventoLuz(planta);
		EventoLuz apagar = new EventoLuz(planta);
		indoor.recibirEvento(prender);
		indoor.recibirEvento(apagar);

		empleado.atenderEvento(prender);
		assertTrue(planta.isLuz());

		empleado.atenderEvento(apagar);
		assertFalse(planta.isLuz());
	}

	// Atender un evento de ventilador prende el ventilador de la planta
	@Test
	public void unEventoDeVentiladorCambiaElEstadoDelVentilador() {
		EventoVentilador ventilador = new EventoVentilador(planta);
		indoor.recibirEvento(ventilador);

		empleado.atenderEvento(ventilador);

		assertTrue(planta.isVentilador());
	}

	// Atender dos veces el mismo evento se rechaza y su efecto no se aplica de nuevo
	@Test
	public void noSePuedeAtenderDosVecesElMismoEvento() {
		EventoLuz luz = new EventoLuz(planta);
		indoor.recibirEvento(luz);
		empleado.atenderEvento(luz);

		assertThrows(OperacionInvalidaException.class, () -> empleado.atenderEvento(luz));

		assertTrue(planta.isLuz());
	}

	// Un empleado no puede atender eventos de un indoor que no tiene a cargo, y el evento no cambia
	@Test
	public void noSePuedeAtenderUnEventoDeUnIndoorNoAsignado() {
		Indoor otroIndoor = DatosDePrueba.indoor();
		Planta otraPlanta = otroIndoor.addPlanta(nuevaPlanta("Amnesia"));
		EventoLuz luz = new EventoLuz(otraPlanta);
		otroIndoor.recibirEvento(luz);

		assertThrows(OperacionInvalidaException.class, () -> empleado.atenderEvento(luz));

		assertFalse(luz.getRealizado());
		assertFalse(otraPlanta.isLuz());
	}

	// Los pendientes del empleado son solo los eventos sin atender de sus indoors
	@Test
	public void losPendientesSonLosNoAtendidosDeSusIndoors() {
		EventoRegado riego = new EventoRegado(planta);
		EventoLuz luz = new EventoLuz(planta);
		indoor.recibirEvento(riego);
		indoor.recibirEvento(luz);

		Indoor otroIndoor = DatosDePrueba.indoor();
		otroIndoor.recibirEvento(new EventoLuz(otroIndoor.addPlanta(nuevaPlanta("Amnesia"))));

		empleado.atenderEvento(riego);

		assertEquals(1, empleado.eventosPendientes().size());
		assertSame(luz, empleado.eventosPendientes().get(0));
	}

	private Planta nuevaPlanta(String genetica) {
		return new Planta(genetica, LocalDate.now().minusDays(10), LocalDate.now().minusDays(20), 60, 120, 30);
	}
}
