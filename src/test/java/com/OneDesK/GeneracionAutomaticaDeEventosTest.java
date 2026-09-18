package com.OneDesK;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import com.OneDesK.services.IndoorService;
import com.OneDesK.services.TaskService;

// Levanta la aplicacion completa contra MySQL con el scheduler encendido y un cron de cada 1 segundo:
// nadie llama al generador, lo dispara el hilo del scheduler igual que en produccion.
// Los datos se guardan de verdad (no hay rollback) y cada test borra los suyos al terminar.
// DirtiesContext cierra esta aplicacion al final de la clase, asi el scheduler no sigue corriendo
// durante el resto de los tests.
@SpringBootTest(properties = { "onedesk.eventos.activo=true", "onedesk.eventos.cron=* * * * * *" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class GeneracionAutomaticaDeEventosTest {

	@Autowired
	private IndoorService indoorService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private JdbcTemplate jdbc;

	private IndoorDePrueba indoor;

	@BeforeEach
	public void setUp() {
		indoor = new IndoorDePrueba(indoorService, jdbc);
	}

	@AfterEach
	public void borrarDatos() throws InterruptedException {
		indoor.borrar();
	}

	// --- el scheduler con el generador de eventos ---

	// Con la ventilacion vencida, el scheduler crea solo el evento en la base en la siguiente vuelta
	@Test
	public void elSchedulerCreaElEventoEnLaBase() throws InterruptedException {
		indoor.ventilacionAtendidaHace(30);

		assertTrue(indoor.esperarPendientes(1, 5), "El scheduler no creo el evento en 5 segundos");
	}

	// Con el tiempo sin cumplir, varias vueltas del scheduler no crean nada
	@Test
	public void elSchedulerNoCreaNadaAntesDeTiempo() throws InterruptedException {
		indoor.ventilacionAtendidaHace(29);

		Thread.sleep(3000);

		assertEquals(0, indoor.eventosPendientes());
	}

	// Despues de crear el evento, las vueltas siguientes del scheduler no lo duplican
	@Test
	public void elSchedulerNoDuplicaElEventoPendiente() throws InterruptedException {
		indoor.ventilacionAtendidaHace(30);
		assertTrue(indoor.esperarPendientes(1, 5));

		Thread.sleep(3000);

		assertEquals(1, indoor.eventosPendientes());
	}

	// --- el TaskService con el scheduler de Spring ---

	// Una tarea programada corre sola en un hilo del pool de Spring, no en el hilo que la programo
	@Test
	public void unaTareaProgramadaCorreEnUnHiloDelScheduler() throws InterruptedException {
		CountDownLatch corrio = new CountDownLatch(1);
		String[] hilo = new String[1];

		taskService.scheduleTask(() -> {
			hilo[0] = Thread.currentThread().getName();
			corrio.countDown();
		}, "* * * * * *");

		assertTrue(corrio.await(5, TimeUnit.SECONDS), "La tarea no corrio en 5 segundos");
		assertTrue(hilo[0].startsWith("scheduling-"), "Corrio en el hilo " + hilo[0]);
	}

	// Si una vuelta tira una excepcion (por ejemplo, la base caida) la tarea sigue programada
	// y vuelve a correr: un error no apaga la generacion de eventos para siempre
	@Test
	public void unErrorEnUnaVueltaNoCancelaLasSiguientes() throws InterruptedException {
		AtomicInteger vueltas = new AtomicInteger();
		CountDownLatch dosVueltas = new CountDownLatch(2);

		taskService.scheduleTask(() -> {
			dosVueltas.countDown();
			if (vueltas.incrementAndGet() == 1) {
				throw new IllegalStateException("falla a proposito en la primera vuelta");
			}
		}, "* * * * * *");

		assertTrue(dosVueltas.await(5, TimeUnit.SECONDS), "Despues del error la tarea no volvio a correr");
	}

	// Una expresion cron invalida se rechaza al programarla
	@Test
	public void unCronInvalidoSeRechaza() {
		assertThrows(IllegalArgumentException.class, () -> taskService.scheduleTask(() -> { }, "cada un minuto"));
	}
}
