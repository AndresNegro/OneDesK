package com.OneDesK;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.OneDesK.services.CompraService;
import com.OneDesK.services.EmpleadoIndoorService;
import com.OneDesK.services.GeneradorDeEventosService;
import com.OneDesK.services.IndoorService;
import com.OneDesK.services.ProductoService;
import com.OneDesK.services.RegistroProduccionService;
import com.OneDesK.services.TaskService;
import com.OneDesK.services.UsuarioService;

// Levanta la aplicacion completa sobre MySQL: falla si algun service o repositorio no se puede crear o inyectar.
// La generacion automatica de eventos esta apagada: se comprueba contra la base que asi no escribe nada.
@SpringBootTest(properties = { "onedesk.eventos.activo=false", "onedesk.eventos.cron=* * * * * *" })
public class ContextoSpringTest {

	@Autowired
	private CompraService compraService;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private ProductoService productoService;
	@Autowired
	private IndoorService indoorService;
	@Autowired
	private RegistroProduccionService registroProduccionService;
	@Autowired
	private EmpleadoIndoorService empleadoIndoorService;
	@Autowired
	private GeneradorDeEventosService generadorDeEventosService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private JdbcTemplate jdbc;

	// Levanta la aplicacion completa y verifica que Spring pueda crear e inyectar todos los services,
	// incluido el TaskService, que necesita el TaskScheduler que crea @EnableScheduling
	@Test
	public void laAplicacionLevantaConTodosSusServices() {
		assertNotNull(compraService);
		assertNotNull(usuarioService);
		assertNotNull(productoService);
		assertNotNull(indoorService);
		assertNotNull(registroProduccionService);
		assertNotNull(empleadoIndoorService);
		assertNotNull(generadorDeEventosService);
		assertNotNull(taskService);
	}

	// Con onedesk.eventos.activo=false no se programa la revision: aunque la ventilacion este vencida
	// y el cron sea de cada segundo, pasan varios segundos y en la base no aparece ningun evento
	@Test
	public void conLaGeneracionApagadaNoSeCreanEventos() throws InterruptedException {
		IndoorDePrueba indoor = new IndoorDePrueba(indoorService, jdbc);
		try {
			indoor.ventilacionAtendidaHace(30);

			Thread.sleep(3000);

			assertEquals(0, indoor.eventosPendientes());
		} finally {
			indoor.borrar();
		}
	}
}
