package com.OneDesK.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.OneDesK.modelo.Administrador;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Usuario;

// Cada rol sabe que puede hacer, y cada persona toma el suyo al crearse.
public class RolTest {

	// El cliente compra y nada mas: no atiende el indoor ni administra
	@Test
	public void elUsuarioSoloPuedeComprar() {
		assertTrue(Rol.USUARIO.puede(Permiso.COMPRAR));
		assertFalse(Rol.USUARIO.puede(Permiso.CUIDAR_PLANTAS));
		assertFalse(Rol.USUARIO.puede(Permiso.ADMINISTRAR_NEGOCIO));
	}

	// El empleado cuida las plantas de su indoor, pero no entra a la administracion
	@Test
	public void elEmpleadoSoloCuidaPlantas() {
		assertTrue(Rol.EMPLEADO.puede(Permiso.CUIDAR_PLANTAS));
		assertFalse(Rol.EMPLEADO.puede(Permiso.ADMINISTRAR_NEGOCIO));
		assertFalse(Rol.EMPLEADO.puede(Permiso.COMPRAR));
	}

	// El administrador administra, y tampoco compra: el catalogo es para los clientes
	@Test
	public void elAdministradorAdministra() {
		assertTrue(Rol.ADMINISTRADOR.puede(Permiso.ADMINISTRAR_NEGOCIO));
		assertFalse(Rol.ADMINISTRADOR.puede(Permiso.COMPRAR));
	}

	// El rol se lo pone la persona al construirse, y es ella la que contesta que puede hacer
	@Test
	public void cadaPersonaTomaSuRol() {
		Usuario cliente = new Usuario("Juan", "Perez", "juan@test.com", "12345");
		EmpleadoIndoor empleado = new EmpleadoIndoor("Ana", "Lopez", "ana@test.com", "12345", 100000);
		Administrador administrador = new Administrador("Eva", "Diaz", "eva@test.com", "12345");

		assertEquals(Rol.USUARIO, cliente.getRol());
		assertEquals(Rol.EMPLEADO, empleado.getRol());
		assertEquals(Rol.ADMINISTRADOR, administrador.getRol());
		assertTrue(cliente.puede(Permiso.COMPRAR));
		assertFalse(cliente.puede(Permiso.ADMINISTRAR_NEGOCIO));
		assertTrue(empleado.puede(Permiso.CUIDAR_PLANTAS));
	}

	// Los roles de una persona no se agregan desde afuera: los toma ella al construirse
	@Test
	public void losRolesDeUnaPersonaNoSeTocanDesdeAfuera() {
		Usuario cliente = new Usuario("Juan", "Perez", "juan@test.com", "12345");

		assertThrows(UnsupportedOperationException.class, () -> cliente.getRoles().add(Rol.ADMINISTRADOR));
	}

	// El id con el que el rol viaja a la tabla Rol esta escrito, no sale del orden del enum
	@Test
	public void elRolVaYVuelveDeLaBasePorSuId() {
		assertEquals(Rol.USUARIO, Rol.delId(Rol.USUARIO.getId()));
		assertEquals(Rol.EMPLEADO, Rol.delId(Rol.EMPLEADO.getId()));
		assertEquals(Rol.ADMINISTRADOR, Rol.delId(Rol.ADMINISTRADOR.getId()));
		assertThrows(IllegalArgumentException.class, () -> Rol.delId(99));
	}

	// Los permisos de un rol no se modifican desde afuera: se cambian en el enum y en ningun otro lado
	@Test
	public void losPermisosNoSeModificanDesdeAfuera() {
		assertThrows(UnsupportedOperationException.class,
				() -> Rol.USUARIO.getPermisos().add(Permiso.ADMINISTRAR_NEGOCIO));
	}
}
