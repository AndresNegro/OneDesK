package com.OneDesK.services;

/** Los avisos que la aplicacion le manda por mail a quien pidio una cuenta. */
public interface CorreoService {

	void avisarQueLaCuentaFueAprobada(String nombre, String email, int topeCredito);

	void avisarQueLaSolicitudFueRechazada(String nombre, String email);
}
