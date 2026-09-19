package com.OneDesK.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.OneDesK.modelo.LimitesDeCompra;

// Los limites de gramos contra MySQL: hay una sola fila de configuracion y se crea sola la primera vez
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LimitesDeCompraServiceImpl.class)
public class LimitesDeCompraServiceImplTest {

	@Autowired
	private LimitesDeCompraService service;
	@Autowired
	private TestEntityManager em;

	// se arranca sin fila guardada, como una base recien creada; el borrado se deshace al terminar el test
	@BeforeEach
	public void setUp() {
		em.getEntityManager().createNativeQuery("DELETE FROM LimitesDeCompra").executeUpdate();
		em.clear();
	}

	// Sin configuracion guardada, obtener la crea con 5 g y 40 g
	@Test
	public void laPrimeraVezSeCreanLosLimitesIniciales() {
		LimitesDeCompra limites = service.obtener();
		em.flush();

		assertEquals(5, limites.getMinimoGramos());
		assertEquals(40, limites.getMaximoGramos());
		assertEquals(1, contarFilas());
	}

	// Pedirlos varias veces no crea filas nuevas: siempre es la misma configuracion
	@Test
	public void siempreHayUnaSolaFila() {
		service.obtener();
		em.flush();
		service.obtener();
		service.cambiar(10, 30);
		em.flush();

		assertEquals(1, contarFilas());
	}

	// Cambiarlos queda guardado en la base
	@Test
	public void cambiarlosQuedaGuardado() {
		service.cambiar(10, 30);
		em.flush();
		em.clear();

		LimitesDeCompra recargados = service.obtener();
		assertEquals(10, recargados.getMinimoGramos());
		assertEquals(30, recargados.getMaximoGramos());
	}

	// Valores invalidos se rechazan y en la base quedan los anteriores
	@Test
	public void valoresInvalidosNoSeGuardan() {
		service.cambiar(10, 30);
		em.flush();
		em.clear();

		assertThrows(IllegalArgumentException.class, () -> service.cambiar(20, 15));
		em.flush();
		em.clear();

		assertEquals(10, service.obtener().getMinimoGramos());
		assertEquals(30, service.obtener().getMaximoGramos());
	}

	private int contarFilas() {
		return ((Number) em.getEntityManager().createNativeQuery("SELECT COUNT(*) FROM LimitesDeCompra")
				.getSingleResult()).intValue();
	}
}
