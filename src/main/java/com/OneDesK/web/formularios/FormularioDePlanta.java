package com.OneDesK.web.formularios;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

/** Una planta nueva: donde va, que genetica es y cada cuanto pide atencion. */
public class FormularioDePlanta {

	private int indoorId;
	private String genetica;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate fechaGerminado;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate fechaPlantado;
	private int tiempoRegado;
	private int tiempoLuz;
	private int tiempoVentilacion;

	public int getIndoorId() {
		return indoorId;
	}

	public void setIndoorId(int indoorId) {
		this.indoorId = indoorId;
	}

	public String getGenetica() {
		return genetica;
	}

	public void setGenetica(String genetica) {
		this.genetica = genetica;
	}

	public LocalDate getFechaGerminado() {
		return fechaGerminado;
	}

	public void setFechaGerminado(LocalDate fechaGerminado) {
		this.fechaGerminado = fechaGerminado;
	}

	public LocalDate getFechaPlantado() {
		return fechaPlantado;
	}

	public void setFechaPlantado(LocalDate fechaPlantado) {
		this.fechaPlantado = fechaPlantado;
	}

	public int getTiempoRegado() {
		return tiempoRegado;
	}

	public void setTiempoRegado(int tiempoRegado) {
		this.tiempoRegado = tiempoRegado;
	}

	public int getTiempoLuz() {
		return tiempoLuz;
	}

	public void setTiempoLuz(int tiempoLuz) {
		this.tiempoLuz = tiempoLuz;
	}

	public int getTiempoVentilacion() {
		return tiempoVentilacion;
	}

	public void setTiempoVentilacion(int tiempoVentilacion) {
		this.tiempoVentilacion = tiempoVentilacion;
	}
}
