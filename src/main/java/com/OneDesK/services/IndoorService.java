package com.OneDesK.services;

import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;

public interface IndoorService {

	public Indoor crearIndoor();

	public Planta plantar(int indoorId, Planta planta);

	/** Quita una planta que no fue cosechada, descartando sus eventos pendientes. */
	public void quitarPlanta(int indoorId, int plantaId);
}
