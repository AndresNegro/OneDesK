package com.OneDesK.services;

public interface GeneradorDeEventosService {

	/**
	 * Revisa todas las plantas en cultivo y crea los eventos cuyo tiempo ya se cumplio a la hora actual.
	 * Devuelve cuantos eventos creo.
	 */
	public int generarEventos();
}
