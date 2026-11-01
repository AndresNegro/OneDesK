package com.OneDesK.dtos;

/**
 * El mismo cliente, pero con lo que solo le importa a la administracion: si ya esta aprobado,
 * cuanto tiene comprometido contando los pedidos que esperan respuesta, y cuantas compras lleva.
 * Hereda de UsuarioDTO para no repetir lo que ya sale por la API publica.
 */
public class UsuarioAdminDTO extends UsuarioDTO {

	private boolean aprobado;
	private int deudaComprometida;
	private int compras;

	public boolean isAprobado() {
		return aprobado;
	}

	public void setAprobado(boolean aprobado) {
		this.aprobado = aprobado;
	}

	public int getDeudaComprometida() {
		return deudaComprometida;
	}

	public void setDeudaComprometida(int deudaComprometida) {
		this.deudaComprometida = deudaComprometida;
	}

	public int getCompras() {
		return compras;
	}

	public void setCompras(int compras) {
		this.compras = compras;
	}
}
