package com.OneDesK.services;

import java.util.List;

import com.OneDesK.evento.Evento;
import com.OneDesK.modelo.EmpleadoIndoor;

public interface EmpleadoIndoorService {

	public EmpleadoIndoor registrar(String nombre, String apellido, String email, String contrasenia,
			int salarioMensual);

	public EmpleadoIndoor cambiarSalario(int empleadoId, int salarioMensual);

	public void asignarIndoor(int empleadoId, int indoorId);

	public void desasignarIndoor(int empleadoId, int indoorId);

	public List<Evento> eventosPendientes(int empleadoId);

	/** Aplica el efecto del evento sobre su planta y lo deja guardado como realizado. */
	public void atenderEvento(int empleadoId, int indoorId, int eventoId);
}
