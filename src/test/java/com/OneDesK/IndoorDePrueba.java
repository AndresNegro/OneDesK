package com.OneDesK;

import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;

import com.OneDesK.modelo.Planta;
import com.OneDesK.services.IndoorService;

/**
 * Un indoor con una planta, creado con los services reales y guardado de verdad en MySQL.
 * Lo usan los tests que levantan la aplicacion completa: ahi no hay transaccion de test que
 * deshaga los cambios, asi que cada test lo borra al terminar con borrar().
 * La planta usa riego cada 60 minutos, luz cada 120 y ventilacion cada 30.
 */
class IndoorDePrueba {

	private final JdbcTemplate jdbc;
	private final int idIndoor;
	private final int idPlanta;

	IndoorDePrueba(IndoorService indoorService, JdbcTemplate jdbc) {
		this.jdbc = jdbc;
		this.idIndoor = indoorService.crearIndoor(DatosDePrueba.nombreDeIndoor(), DatosDePrueba.CAPACIDAD).getId();
		Planta planta = new Planta("PRUEBA SCHEDULER", LocalDate.now().minusDays(10),
				LocalDate.now().minusDays(20), 60, 120, 30);
		this.idPlanta = indoorService.plantar(idIndoor, planta).getId();
	}

	/** Guarda en la base que la ventilacion se atendio por ultima vez hace esa cantidad de minutos. */
	void ventilacionAtendidaHace(int minutos) {
		jdbc.update("UPDATE Planta SET ultimoVentilacion = NOW() - INTERVAL ? MINUTE WHERE ID = ?", minutos, idPlanta);
	}

	int eventosPendientes() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM Evento WHERE ID_PLANTA = ? AND realizado = 0",
				Integer.class, idPlanta);
	}

	/** Espera hasta que la planta tenga esa cantidad de eventos pendientes; devuelve false si no llega a tiempo. */
	boolean esperarPendientes(int cantidad, int segundos) throws InterruptedException {
		long limite = System.currentTimeMillis() + segundos * 1000L;
		while (System.currentTimeMillis() < limite) {
			if (eventosPendientes() == cantidad) {
				return true;
			}
			Thread.sleep(200);
		}
		return eventosPendientes() == cantidad;
	}

	/**
	 * Borra el indoor, la planta y sus eventos. Primero marca la planta como cosechada para que el
	 * scheduler, que sigue corriendo en otro hilo, deje de crearle eventos mientras se borra.
	 */
	void borrar() throws InterruptedException {
		jdbc.update("UPDATE Planta SET fechaCosecha = CURDATE() WHERE ID = ?", idPlanta);
		// una vuelta del scheduler que ya estaba en curso puede terminar de insertar su evento: se reintenta
		for (int intento = 1; ; intento++) {
			try {
				jdbc.update("DELETE FROM Evento WHERE ID_PLANTA = ?", idPlanta);
				jdbc.update("DELETE FROM Planta WHERE ID = ?", idPlanta);
				jdbc.update("DELETE FROM Indoor WHERE ID = ?", idIndoor);
				return;
			} catch (RuntimeException e) {
				if (intento == 3) {
					throw e;
				}
				Thread.sleep(1500);
			}
		}
	}
}
