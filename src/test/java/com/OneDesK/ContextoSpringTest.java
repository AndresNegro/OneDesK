package com.OneDesK;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.OneDesK.services.CompraService;
import com.OneDesK.services.EmpleadoIndoorService;
import com.OneDesK.services.IndoorService;
import com.OneDesK.services.ProductoService;
import com.OneDesK.services.RegistroProduccionService;
import com.OneDesK.services.UsuarioService;

// Levanta la aplicacion completa sobre H2: falla si algun service o repositorio no se puede crear o inyectar
@SpringBootTest
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

	// Levanta la aplicacion completa sobre H2 y verifica que Spring pueda crear e inyectar los seis services
	@Test
	public void laAplicacionLevantaConTodosSusServices() {
		assertNotNull(compraService);
		assertNotNull(usuarioService);
		assertNotNull(productoService);
		assertNotNull(indoorService);
		assertNotNull(registroProduccionService);
		assertNotNull(empleadoIndoorService);
	}
}
