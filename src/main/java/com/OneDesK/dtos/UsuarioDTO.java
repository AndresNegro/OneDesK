package com.OneDesK.dtos;

/** Un cliente, como lo ve quien consume la API. */
public class UsuarioDTO extends BaseDTO {

	private String email;
	private String nombreCompleto;
	private int deuda;
	private int topeCredito;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getNombreCompleto() {
		return nombreCompleto;
	}

	public void setNombreCompleto(String nombreCompleto) {
		this.nombreCompleto = nombreCompleto;
	}

	public int getDeuda() {
		return deuda;
	}

	public void setDeuda(int deuda) {
		this.deuda = deuda;
	}

	public int getTopeCredito() {
		return topeCredito;
	}

	public void setTopeCredito(int topeCredito) {
		this.topeCredito = topeCredito;
	}
}
