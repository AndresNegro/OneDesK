package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.OneDesK.evento.Evento;
import com.OneDesK.evento.EventoLuz;
import com.OneDesK.evento.EventoRegado;
import com.OneDesK.evento.EventoVentilador;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;

// Generacion de eventos contra MySQL, con la hora real y sin nada simulado.
// Para que "pase el tiempo" se guarda en la base una ultima atencion vieja (por ejemplo, hace 30 minutos)
// y despues se corre la revision: asi el tiempo transcurrido es real segun el reloj de la base.
// Cada test revisa su propio indoor recargandolo, porque la revision recorre todos los de la base.
// La planta usa riego cada 60 minutos, luz cada 120 y ventilacion cada 30.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ GeneradorDeEventosServiceImpl.class, IndoorServiceImpl.class, EmpleadoIndoorServiceImpl.class })
public class GeneradorDeEventosServiceImplTest {

	@Autowired
	private GeneradorDeEventosService service;
	@Autowired
	private IndoorService indoorService;
	@Autowired
	private EmpleadoIndoorService empleadoService;
	@Autowired
	private TestEntityManager em;

	private int idIndoor;
	private int idPlanta;

	@BeforeEach
	public void setUp() {
		idIndoor = indoorService.crearIndoor().getId();
		idPlanta = indoorService.plantar(idIndoor, nuevaPlanta("OG Kush")).getId();
		recargar();
	}

	// --- cuando empieza a correr el tiempo ---

	// Al plantar, los tres tiempos quedan guardados con la hora actual de la base
	@Test
	public void alPlantarLosTiemposEmpiezanConLaHoraDeLaBase() {
		assertTrue(segundosDesde("ultimoRegado") <= 5);
		assertTrue(segundosDesde("ultimoLuz") <= 5);
		assertTrue(segundosDesde("ultimoVentilacion") <= 5);
	}

	// Una planta recien plantada no genera nada: ninguno de sus tiempos se cumplio
	@Test
	public void unaPlantaRecienPlantadaNoGeneraNada() {
		service.generarEventos();
		recargar();

		assertTrue(indoor().getColaEventos().isEmpty());
	}

	// --- cuando se crea un evento ---

	// Si la ventilacion se atendio hace 29 minutos todavia falta 1, y no se crea nada
	@Test
	public void antesDeCumplirseElTiempoNoSeCreaNada() {
		ultimaAtencionHace("ultimoVentilacion", 29);

		service.generarEventos();
		recargar();

		assertTrue(indoor().getColaEventos().isEmpty());
	}

	// Si la ventilacion se atendio hace 30 minutos se cumplio su tiempo y queda guardado un evento pendiente
	@Test
	public void alCumplirseElTiempoSeGuardaElEvento() {
		ultimaAtencionHace("ultimoVentilacion", 30);

		service.generarEventos();
		recargar();

		List<Evento> eventos = indoor().getColaEventos();
		assertEquals(1, eventos.size());
		assertTrue(eventos.get(0) instanceof EventoVentilador);
		assertFalse(eventos.get(0).getRealizado());
	}

	// Cada tipo respeta su propio tiempo: pasada 1 hora hay riego y ventilacion, pero la luz todavia no
	@Test
	public void cadaTipoRespetaSuPropioTiempo() {
		todoAtendidoHace(60);

		service.generarEventos();
		recargar();

		assertEquals(1, pendientes(EventoRegado.class));
		assertEquals(0, pendientes(EventoLuz.class));
		assertEquals(1, pendientes(EventoVentilador.class));
	}

	// Pasadas 2 horas se cumplieron los tres tiempos y se guarda un evento de cada tipo
	@Test
	public void conTodosLosTiemposCumplidosSeGuardaUnoDeCadaTipo() {
		todoAtendidoHace(120);

		service.generarEventos();
		recargar();

		assertEquals(3, indoor().getColaEventos().size());
		assertEquals(1, pendientes(EventoRegado.class));
		assertEquals(1, pendientes(EventoLuz.class));
		assertEquals(1, pendientes(EventoVentilador.class));
	}

	// --- sin duplicados ---

	// Mientras el evento no se atiende, las revisiones siguientes no guardan otro del mismo tipo
	@Test
	public void unEventoSinAtenderNoSeDuplica() {
		ultimaAtencionHace("ultimoVentilacion", 45);

		service.generarEventos();
		recargar();
		service.generarEventos();
		recargar();
		service.generarEventos();
		recargar();

		assertEquals(1, indoor().getColaEventos().size());
	}

	// El pendiente de una planta no frena a otra planta del mismo indoor: cada una recibe el suyo
	@Test
	public void elPendienteDeUnaPlantaNoFrenaAOtra() {
		int idOtra = indoorService.plantar(idIndoor, nuevaPlanta("Amnesia")).getId();
		recargar();
		ultimaAtencionHace("ultimoVentilacion", 30);
		service.generarEventos();
		recargar();

		ultimaAtencionHace(idOtra, "ultimoVentilacion", 30);
		service.generarEventos();
		recargar();

		assertEquals(2, pendientes(EventoVentilador.class));
		assertEquals(1, eventosDe(idPlanta));
		assertEquals(1, eventosDe(idOtra));
	}

	// --- atender reinicia el tiempo ---

	// Al atender, el tiempo vuelve a correr desde la atencion: recien 30 minutos despues aparece el siguiente
	@Test
	public void despuesDeAtenderElTiempoCorreDesdeLaAtencion() {
		int idEmpleado = empleadoAsignado();
		ultimaAtencionHace("ultimoVentilacion", 30);
		service.generarEventos();
		recargar();

		empleadoService.atenderEvento(idEmpleado, idIndoor, indoor().getEventosPendientes().get(0).getId());
		recargar();
		assertTrue(segundosDesde("ultimoVentilacion") <= 5);

		// recien atendido: el evento atendido queda como historial y no se crea otro
		service.generarEventos();
		recargar();
		assertEquals(1, indoor().getColaEventos().size());
		assertEquals(0, indoor().getEventosPendientes().size());

		// 30 minutos despues de la atencion vuelve a tocar
		ultimaAtencionHace("ultimoVentilacion", 30);
		service.generarEventos();
		recargar();
		assertEquals(2, indoor().getColaEventos().size());
		assertEquals(1, indoor().getEventosPendientes().size());
	}

	// Atender la ventilacion reinicia solo la ventilacion: riego y luz siguen contando desde antes
	@Test
	public void atenderUnTipoNoReiniciaLosOtros() {
		int idEmpleado = empleadoAsignado();
		todoAtendidoHace(40);
		service.generarEventos();
		recargar();

		empleadoService.atenderEvento(idEmpleado, idIndoor, indoor().getEventosPendientes().get(0).getId());
		recargar();

		assertTrue(segundosDesde("ultimoVentilacion") <= 5);
		assertTrue(segundosDesde("ultimoRegado") >= 40 * 60);
		assertTrue(segundosDesde("ultimoLuz") >= 40 * 60);
	}

	// --- plantas que no generan ---

	// Una planta cosechada no recibe eventos aunque sus tiempos esten cumplidos hace un dia
	@Test
	public void unaPlantaCosechadaNoGeneraEventos() {
		todoAtendidoHace(24 * 60);
		em.find(Planta.class, idPlanta).cosechar();
		recargar();

		service.generarEventos();
		recargar();

		assertTrue(indoor().getColaEventos().isEmpty());
	}

	// --- helpers ---

	// guarda en la base que la ultima atencion de ese tipo fue hace esa cantidad de minutos
	private void ultimaAtencionHace(String columna, int minutos) {
		ultimaAtencionHace(idPlanta, columna, minutos);
	}

	private void ultimaAtencionHace(int plantaId, String columna, int minutos) {
		em.flush();
		em.getEntityManager().createNativeQuery("UPDATE Planta SET " + columna + " = NOW() - INTERVAL "
				+ minutos + " MINUTE WHERE ID = " + plantaId).executeUpdate();
		em.clear();
	}

	private void todoAtendidoHace(int minutos) {
		ultimaAtencionHace("ultimoRegado", minutos);
		ultimaAtencionHace("ultimoLuz", minutos);
		ultimaAtencionHace("ultimoVentilacion", minutos);
	}

	// cuantos segundos pasaron, segun el reloj de MySQL, desde el valor guardado en esa columna
	private long segundosDesde(String columna) {
		Number segundos = (Number) em.getEntityManager().createNativeQuery(
				"SELECT TIMESTAMPDIFF(SECOND, " + columna + ", NOW()) FROM Planta WHERE ID = " + idPlanta)
				.getSingleResult();
		return segundos.longValue();
	}

	private int empleadoAsignado() {
		EmpleadoIndoor empleado = empleadoService.registrar("Andres", "Negro", "empleado@test.com", "12345", 500000);
		empleadoService.asignarIndoor(empleado.getId(), idIndoor);
		recargar();
		return empleado.getId();
	}

	private int pendientes(Class<? extends Evento> tipo) {
		int cantidad = 0;
		for (Evento evento : indoor().getEventosPendientes()) {
			if (tipo.isInstance(evento)) {
				cantidad++;
			}
		}
		return cantidad;
	}

	private long eventosDe(int plantaId) {
		return ((Number) em.getEntityManager()
				.createNativeQuery("SELECT COUNT(*) FROM Evento WHERE ID_PLANTA = " + plantaId)
				.getSingleResult()).longValue();
	}

	private Indoor indoor() {
		return em.find(Indoor.class, idIndoor);
	}

	private void recargar() {
		em.flush();
		em.clear();
	}

	private Planta nuevaPlanta(String genetica) {
		return new Planta(genetica, LocalDate.now().minusDays(10), LocalDate.now().minusDays(20), 60, 120, 30);
	}
}
