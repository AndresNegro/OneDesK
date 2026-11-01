package com.OneDesK.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import com.OneDesK.DatosDePrueba;
import com.OneDesK.dtos.DTO;
import com.OneDesK.dtos.IndoorDTO;
import com.OneDesK.dtos.ProductoDTO;
import com.OneDesK.dtos.UsuarioAdminDTO;
import com.OneDesK.dtos.UsuarioDTO;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;
import com.OneDesK.modelo.Producto;
import com.OneDesK.modelo.Usuario;

// El mapeador arma el DTO que pide cada entidad. Lo que se copia solo se prueba igual, porque un
// cambio de nombre de un lado rompe el mapeo en silencio: no lo avisa el compilador.
public class ConfiguracionDeMapeoTest {

	private final ModelMapper mapeador = new ConfiguracionDeMapeo().modelMapper();

	// Un producto se copia entero sin configurar nada: los campos se llaman igual de los dos lados
	@Test
	public void elProductoSeMapeaSolo() {
		Producto producto = new Producto("OG Kush", 12, 1000);

		ProductoDTO dto = mapeador.map(producto, ProductoDTO.class);

		assertEquals("OG Kush", dto.getGenetica());
		assertEquals(12, dto.getStock());
		assertEquals(1000, dto.getPrecio());
		assertEquals(ProductoDTO.class, producto.dtoParaApi());
	}

	// En el cliente, el nombre completo se copia solo y la deuda la completa el mapeo configurado
	@Test
	public void elClienteSeMapeaConSuDTO() {
		Usuario usuario = new Usuario("Andrés", "Negro", "andres@test.com", "12345");
		usuario.aprobar(5000);

		UsuarioDTO dto = (UsuarioDTO) mapeador.map(usuario, usuario.dtoParaApi());

		assertEquals(UsuarioDTO.class, usuario.dtoParaApi());
		assertEquals("andres@test.com", dto.getEmail());
		assertEquals("Andrés Negro", dto.getNombreCompleto());
		assertEquals(5000, dto.getTopeCredito());
		assertEquals(0, dto.getDeuda());
	}

	// El DTO del administrador suma lo suyo, que tambien sale del mapeo configurado
	@Test
	public void elClienteParaElAdministradorTraeSusDatosDeMas() {
		Usuario usuario = new Usuario("Andrés", "Negro", "andres@test.com", "12345");
		usuario.aprobar(5000);

		UsuarioAdminDTO dto = (UsuarioAdminDTO) mapeador.map(usuario, usuario.dtoParaAdmin());

		assertEquals(UsuarioAdminDTO.class, usuario.dtoParaAdmin());
		// lo que ya traia el DTO publico, que hereda
		assertEquals("Andrés Negro", dto.getNombreCompleto());
		assertEquals(5000, dto.getTopeCredito());
		// y lo que agrega el del administrador
		assertTrue(dto.isAprobado());
		assertEquals(0, dto.getDeudaComprometida());
		assertEquals(0, dto.getCompras());
	}

	// En el indoor, las plantas en cultivo y los eventos pendientes son cuentas, no copias
	@Test
	public void elIndoorTraeSusCuentas() {
		Indoor indoor = DatosDePrueba.indoor();
		indoor.addPlanta(new Planta("OG Kush", LocalDate.now().minusDays(10), LocalDate.now().minusDays(20), 60, 120, 30));

		IndoorDTO dto = (IndoorDTO) mapeador.map(indoor, indoor.dtoParaApi());

		assertEquals(indoor.getNombre(), dto.getNombre());
		assertEquals(DatosDePrueba.CAPACIDAD, dto.getCapacidad());
		assertEquals(1, dto.getPlantasEnCultivo());
		assertEquals(0, dto.getEventosPendientes());
	}
}
