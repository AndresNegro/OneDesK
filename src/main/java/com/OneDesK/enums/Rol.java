package com.OneDesK.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Que es una persona dentro del negocio y que puede hacer. El id es el de la tabla Rol: esta
 * escrito aca y no se deduce del orden, asi reordenar el enum no cambia lo que ya esta guardado.
 */
public enum Rol {

	USUARIO(1, Permiso.COMPRAR),
	EMPLEADO(2, Permiso.CUIDAR_PLANTAS),
	ADMINISTRADOR(3, Permiso.ADMINISTRAR_NEGOCIO);

	private final int id;
	private final Set<Permiso> permisos;

	Rol(int id, Permiso... permisos) {
		this.id = id;
		this.permisos = EnumSet.copyOf(List.of(permisos));
	}

	public int getId() {
		return id;
	}

	/** Lo pregunta el rol y no quien lo mira: afuera no se recorre la lista de permisos. */
	public boolean puede(Permiso permiso) {
		return permisos.contains(permiso);
	}

	public Set<Permiso> getPermisos() {
		return Collections.unmodifiableSet(permisos);
	}

	/** El rol que tiene ese id en la tabla Rol, que es como viaja a la base. */
	public static Rol delId(int id) {
		for (Rol rol : values()) {
			if (rol.id == id) {
				return rol;
			}
		}
		throw new IllegalArgumentException("No hay ningun rol con el id " + id);
	}
}
