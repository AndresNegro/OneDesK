package com.OneDesK.web.formularios;

/** Poner un empleado a cargo de un indoor, o sacarlo. */
public class FormularioDeAsignacion {

	private int empleadoId;
	private int indoorId;

	public int getEmpleadoId() {
		return empleadoId;
	}

	public void setEmpleadoId(int empleadoId) {
		this.empleadoId = empleadoId;
	}

	public int getIndoorId() {
		return indoorId;
	}

	public void setIndoorId(int indoorId) {
		this.indoorId = indoorId;
	}
}
