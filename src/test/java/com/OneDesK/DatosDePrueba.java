package com.OneDesK;

import java.util.UUID;

import com.OneDesK.modelo.Indoor;

/** Datos que los tests necesitan distintos cada vez, porque la base no acepta repetidos. */
public final class DatosDePrueba {

	/** Capacidad amplia, para que ningun test choque con el limite de plantas salvo que lo pruebe a proposito. */
	public static final int CAPACIDAD = 100;

	private DatosDePrueba() {
	}

	/** Un nombre de indoor que no se repite, ni entre tests ni con los indoors que ya tenga la base. */
	public static String nombreDeIndoor() {
		return "Indoor de prueba " + UUID.randomUUID().toString().substring(0, 8);
	}

	public static Indoor indoor() {
		return new Indoor(nombreDeIndoor(), CAPACIDAD);
	}
}
