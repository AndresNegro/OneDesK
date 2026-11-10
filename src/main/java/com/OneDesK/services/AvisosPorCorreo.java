package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Avisa por mail lo que resolvio la administracion.
 *
 * Escucha despues de que la transaccion cerro bien: si la aprobacion termina deshaciendose, el mail
 * no sale. Un mail no se puede volver atras, asi que primero se guarda y recien despues se manda.
 */
@Component
public class AvisosPorCorreo {

	@Autowired
	private CorreoService correo;

	@TransactionalEventListener
	public void cuandoSeApruebaUnaCuenta(CuentaAprobada aviso) {
		correo.avisarQueLaCuentaFueAprobada(aviso.getNombre(), aviso.getEmail(), aviso.getTopeCredito());
	}

	@TransactionalEventListener
	public void cuandoSeRechazaUnaSolicitud(SolicitudRechazada aviso) {
		correo.avisarQueLaSolicitudFueRechazada(aviso.getNombre(), aviso.getEmail());
	}
}
