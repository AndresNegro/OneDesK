package com.OneDesK.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.OneDesK.services.AdministradorService;
import com.OneDesK.services.AdministradorServiceImpl;
import com.OneDesK.services.CompraServiceImpl;
import com.OneDesK.services.EmpleadoIndoorServiceImpl;
import com.OneDesK.services.GeneradorDeEventosServiceImpl;
import com.OneDesK.services.IndoorServiceImpl;
import com.OneDesK.services.LimitesDeCompraServiceImpl;
import com.OneDesK.services.ProductoServiceImpl;
import com.OneDesK.services.RegistroProduccionServiceImpl;
import com.OneDesK.services.UsuarioServiceImpl;

// Carga el negocio de ejemplo en onedesk_test, dentro de la transaccion del test que se deshace al final,
// y revisa en la base que quedo un negocio coherente: con historial, pendientes y todo dentro de las reglas.
// Se cuenta solo lo de ejemplo (@onedesk.demo y los tres indoors), por si la base de test tuviera otra cosa.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ DatosDeEjemplo.class, AdministradorServiceImpl.class, IndoorServiceImpl.class,
		EmpleadoIndoorServiceImpl.class, ProductoServiceImpl.class, UsuarioServiceImpl.class, CompraServiceImpl.class,
		RegistroProduccionServiceImpl.class, GeneradorDeEventosServiceImpl.class, LimitesDeCompraServiceImpl.class })
public class DatosDeEjemploTest {

	private static final String DE_EJEMPLO = "SELECT ID FROM Persona WHERE email LIKE '%@onedesk.demo'";
	private static final String INDOORS = "SELECT ID FROM Indoor WHERE nombre IN ('Carpa grande', 'Carpa chica', 'Sala de flora')";

	@Autowired
	private DatosDeEjemplo datos;
	@Autowired
	private PlatformTransactionManager transacciones;
	@Autowired
	private AdministradorService administradorService;
	@Autowired
	private TestEntityManager em;
	@Autowired
	private JdbcTemplate jdbc;

	private boolean marcadaParaDeshacer;

	@BeforeEach
	public void cargar() {
		// la base de test puede tener otro producto con el mismo nombre de alguna prueba anterior: se limpia
		int admin = administradorService.registrar("Ana", "Admin", "admin@datos.test", "12345").getId();
		datos.cargar(admin);
		// si alguna regla salto adentro de la carga, la transaccion queda marcada para deshacerse
		// y en la aplicacion real no se guardaria nada: eso no tiene que pasar nunca
		marcadaParaDeshacer = new TransactionTemplate(transacciones).execute(estado -> estado.isRollbackOnly());
		em.flush();
		em.clear();
	}

	// La carga no deja la transaccion marcada para deshacerse: en la base real se guarda entera
	@Test
	public void laCargaNoQuedaMarcadaParaDeshacerse() {
		assertFalse(marcadaParaDeshacer);
	}

	// Quedan los tres indoors con sus empleados asignados y sin pasarse de la capacidad
	@Test
	public void hayIndoorsConEmpleadosYDentroDeSuCapacidad() {
		assertEquals(3, contar("SELECT COUNT(*) FROM (" + INDOORS + ") i"));
		assertEquals(3, contar("SELECT COUNT(DISTINCT ID_INDOOR) FROM Trabaja WHERE ID_INDOOR IN (" + INDOORS + ")"));
		assertEquals(0, contar("SELECT COUNT(*) FROM Indoor i WHERE i.ID IN (" + INDOORS + ") AND i.capacidad <"
				+ " (SELECT COUNT(*) FROM Planta p WHERE p.ID_INDOOR = i.ID AND p.fechaCosecha IS NULL)"));
	}

	// Hay plantas en cultivo y cosechadas, y cada cosecha tiene su registro de produccion con fecha pasada
	@Test
	public void hayPlantasYCosechasConHistorial() {
		assertEquals(19, contar("SELECT COUNT(*) FROM Planta WHERE ID_INDOOR IN (" + INDOORS + ") AND fechaCosecha IS NULL"));
		assertEquals(8, contar("SELECT COUNT(*) FROM Planta WHERE ID_INDOOR IN (" + INDOORS + ") AND fechaCosecha IS NOT NULL"));
		assertEquals(8, contar("SELECT COUNT(*) FROM RegistroProduccion WHERE ID_INDOOR IN (" + INDOORS + ")"
				+ " AND fechaRegistro < CURDATE()"));
	}

	// Las ocho geneticas estan en el catalogo y ninguna quedo con stock negativo
	@Test
	public void elCatalogoTieneStock() {
		assertEquals(8, contar("SELECT COUNT(*) FROM Producto WHERE genetica IN ('OG Kush', 'Amnesia Haze',"
				+ " 'Blue Dream', 'Kush Mints', 'Gorilla Glue', 'Northern Lights', 'Gelato', 'White Widow')"));
		assertEquals(0, contar("SELECT COUNT(*) FROM Producto WHERE stock < 0"));
		assertTrue(contar("SELECT COUNT(*) FROM Producto WHERE stock > 0") >= 6);
	}

	// Hay clientes aprobados y dos solicitudes de registro esperando respuesta
	@Test
	public void hayClientesYSolicitudesPendientes() {
		assertEquals(8, contar("SELECT COUNT(*) FROM Usuario WHERE aprobado = 1 AND ID IN (" + DE_EJEMPLO + ")"));
		assertEquals(2, contar("SELECT COUNT(*) FROM Usuario WHERE aprobado = 0 AND ID IN (" + DE_EJEMPLO + ")"));
	}

	// Hay compras de varias semanas en todos los estados: pagadas, impagas, pendientes y rechazadas
	@Test
	public void hayComprasEnTodosLosEstados() {
		String deEjemplo = " FROM Compra WHERE ID_USUARIO IN (" + DE_EJEMPLO + ")";
		assertTrue(contar("SELECT COUNT(*)" + deEjemplo) >= 20);
		assertTrue(contar("SELECT COUNT(*)" + deEjemplo + " AND estado = 'APROBADA' AND pagado = 1") > 0);
		assertTrue(contar("SELECT COUNT(*)" + deEjemplo + " AND estado = 'APROBADA' AND pagado = 0") > 0);
		assertTrue(contar("SELECT COUNT(*)" + deEjemplo + " AND estado = 'PENDIENTE'") > 0);
		assertTrue(contar("SELECT COUNT(*)" + deEjemplo + " AND estado = 'RECHAZADA'") > 0);
		assertTrue(contar("SELECT COUNT(*)" + deEjemplo + " AND fechaCompra <= CURDATE() - INTERVAL 30 DAY") > 0);
		// ningun pago quedo antes de su compra ni en el futuro
		assertEquals(0, contar("SELECT COUNT(*)" + deEjemplo + " AND (fechaPago < fechaCompra OR fechaPago > CURDATE())"));
	}

	// La deuda de cada cliente coincide con sus compras aprobadas sin pagar, y nadie se pasa de su tope
	@Test
	public void lasDeudasCuadranYRespetanLosTopes() {
		assertEquals(0, contar("SELECT COUNT(*) FROM Usuario u JOIN Deuda d ON d.ID = u.ID_DEUDA"
				+ " WHERE u.ID IN (" + DE_EJEMPLO + ") AND d.monto <> (SELECT COALESCE(SUM(c.precio), 0) FROM Compra c"
				+ " WHERE c.ID_USUARIO = u.ID AND c.estado = 'APROBADA' AND c.pagado = 0)"));
		assertEquals(0, contar("SELECT COUNT(*) FROM Usuario u JOIN Deuda d ON d.ID = u.ID_DEUDA"
				+ " WHERE u.ID IN (" + DE_EJEMPLO + ") AND d.monto > u.topeCredito"));
	}

	// Quedan eventos para atender y otros ya atendidos por los empleados
	@Test
	public void hayEventosPendientesYAtendidos() {
		String deEjemplo = " FROM Evento WHERE ID_INDOOR IN (" + INDOORS + ")";
		assertTrue(contar("SELECT COUNT(*)" + deEjemplo + " AND realizado = 0") > 0);
		assertTrue(contar("SELECT COUNT(*)" + deEjemplo + " AND realizado = 1") > 0);
	}

	private int contar(String sql) {
		return jdbc.queryForObject(sql, Integer.class);
	}
}
