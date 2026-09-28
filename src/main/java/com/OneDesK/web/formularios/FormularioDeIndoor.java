package com.OneDesK.web.formularios;

/** Crear un indoor o editar el nombre y la capacidad de uno que ya existe. */
public class FormularioDeIndoor {

	private int indoorId;
	private String nombre;
	private int capacidad;

	public int getIndoorId() {
		return indoorId;
	}

	public void setIndoorId(int indoorId) {
		this.indoorId = indoorId;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public int getCapacidad() {
		return capacidad;
	}

	public void setCapacidad(int capacidad) {
		this.capacidad = capacidad;
	}
}
