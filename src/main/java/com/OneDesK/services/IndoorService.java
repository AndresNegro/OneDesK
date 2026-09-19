package com.OneDesK.services;

import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;

public interface IndoorService {

	/** Crea un indoor con un nombre que no puede repetirse y la cantidad de plantas que entran. */
	public Indoor crearIndoor(String nombre, int capacidad);

	/** Cambia el nombre y la capacidad. El nombre sigue siendo unico y la capacidad no baja de lo plantado. */
	public Indoor editarIndoor(int indoorId, String nombre, int capacidad);

	public Planta plantar(int indoorId, Planta planta);

	/** Quita una planta que no fue cosechada, descartando sus eventos pendientes. */
	public void quitarPlanta(int indoorId, int plantaId);
}
