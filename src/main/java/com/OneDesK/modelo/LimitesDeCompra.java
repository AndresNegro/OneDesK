package com.OneDesK.modelo;

import com.OneDesK.excepciones.OperacionInvalidaException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Cuantos gramos puede tener una compra, sumando todas sus geneticas. Hay una sola fila:
 * la configura el administrador y la consulta cada compra.
 */
@Entity
@Table(name = "LimitesDeCompra")
public class LimitesDeCompra extends Persistible {

	public static final int MINIMO_INICIAL = 5;
	public static final int MAXIMO_INICIAL = 40;

	@Column(name = "minimoGramos")
	private int minimoGramos;
	@Column(name = "maximoGramos")
	private int maximoGramos;

	public LimitesDeCompra() {
		this.minimoGramos = MINIMO_INICIAL;
		this.maximoGramos = MAXIMO_INICIAL;
	}

	public int getMinimoGramos() { return minimoGramos; }
	public int getMaximoGramos() { return maximoGramos; }

	/** Cambia los dos a la vez, para no quedar nunca con un minimo mayor al maximo. */
	public void cambiar(int minimoGramos, int maximoGramos) {
		if (minimoGramos < 1) {
			throw new IllegalArgumentException("El mínimo tiene que ser de al menos 1 g");
		}
		if (maximoGramos < minimoGramos) {
			throw new IllegalArgumentException("El máximo no puede ser menor que el mínimo");
		}
		this.minimoGramos = minimoGramos;
		this.maximoGramos = maximoGramos;
	}

	/** Rechaza una compra con menos gramos que el minimo o mas que el maximo. */
	public void verificar(int gramos) {
		if (gramos < minimoGramos || gramos > maximoGramos) {
			throw new OperacionInvalidaException("Cada compra tiene que ser de entre " + minimoGramos + " g y "
					+ maximoGramos + " g en total: pediste " + gramos + " g");
		}
	}
}
