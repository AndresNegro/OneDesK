package com.OneDesK.dtos;

/** Un indoor, como lo ve quien consume la API: cuanto tiene plantado y cuanto le falta atender. */
public class IndoorDTO extends BaseDTO {

	private String nombre;
	private int capacidad;
	private int plantasEnCultivo;
	private int eventosPendientes;

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

	public int getPlantasEnCultivo() {
		return plantasEnCultivo;
	}

	public void setPlantasEnCultivo(int plantasEnCultivo) {
		this.plantasEnCultivo = plantasEnCultivo;
	}

	public int getEventosPendientes() {
		return eventosPendientes;
	}

	public void setEventosPendientes(int eventosPendientes) {
		this.eventosPendientes = eventosPendientes;
	}
}
