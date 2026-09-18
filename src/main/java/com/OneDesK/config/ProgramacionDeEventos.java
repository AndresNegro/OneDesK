package com.OneDesK.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.OneDesK.services.GeneradorDeEventosService;
import com.OneDesK.services.TaskService;

/**
 * Programa la revision periodica de las plantas: cada vez que se cumple el cron, un hilo del
 * pool de Spring llama al generador de eventos.
 */
@Configuration
@EnableScheduling
public class ProgramacionDeEventos {

	private static final Logger log = LoggerFactory.getLogger(ProgramacionDeEventos.class);

	@Autowired
	private TaskService taskService;
	@Autowired
	private GeneradorDeEventosService generador;

	// segundos minutos horas dia mes dia-de-la-semana: por defecto, cada 5 minutos
	@Value("${onedesk.eventos.cron:0 */5 * * * *}")
	private String cron;

	// permite apagar la generacion, por ejemplo en el test que levanta la aplicacion entera
	@Value("${onedesk.eventos.activo:true}")
	private boolean activo;

	// se programa recien cuando la aplicacion termino de arrancar, con la base ya disponible
	@EventListener(ApplicationReadyEvent.class)
	public void programar() {
		if (!activo) {
			log.info("Generacion automatica de eventos desactivada");
			return;
		}
		taskService.scheduleTask(this::revisarPlantas, cron);
		log.info("Generacion automatica de eventos programada con cron '{}'", cron);
	}

	// corre en un hilo del scheduler, no en el de una request: por eso delega en un service transaccional
	private void revisarPlantas() {
		int creados = generador.generarEventos();
		if (creados > 0) {
			log.info("Se generaron {} eventos nuevos", creados);
		}
	}
}
