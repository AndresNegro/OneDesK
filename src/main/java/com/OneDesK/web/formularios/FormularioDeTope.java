package com.OneDesK.web.formularios;

/** El tope de credito de un cliente, al aprobarlo o mas adelante. */
public class FormularioDeTope {

	private int usuarioId;
	private int tope;

	public int getUsuarioId() {
		return usuarioId;
	}

	public void setUsuarioId(int usuarioId) {
		this.usuarioId = usuarioId;
	}

	public int getTope() {
		return tope;
	}

	public void setTope(int tope) {
		this.tope = tope;
	}
}
