package com.OneDesK.services;

import com.OneDesK.modelo.RegistroProduccion;

public interface RegistroProduccionService {

	/** Cosecha una planta: suma lo producido al stock del producto de su genetica y deja el registro. */
	public RegistroProduccion registrarCosecha(int empleadoId, int indoorId, int plantaId, int cantidad);
}
