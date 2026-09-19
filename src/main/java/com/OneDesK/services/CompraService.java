package com.OneDesK.services;

import java.util.List;

import com.OneDesK.modelo.Compra;

public interface CompraService {

	/**
	 * El cliente pide una compra: queda pendiente hasta que el administrador la apruebe. El stock se
	 * reserva en ese momento. pagado dice si el cliente eligio pagarla al aprobarse o dejarla en cuenta.
	 */
	public Compra realizarCompra(int usuarioId, List<LineaCompra> lineas, boolean pagado);

	/** El administrador acepta la compra: se cobra o pasa a la cuenta corriente segun lo que eligio el cliente. */
	public Compra aprobarCompra(int compraId);

	/** El administrador rechaza la compra: queda como rechazada y el stock vuelve al catalogo. */
	public Compra rechazarCompra(int compraId);

	/** Las compras que esperan que el administrador las acepte, de la mas vieja a la mas nueva. */
	public List<Compra> comprasPendientes();

	public Compra registrarPago(int compraId);

	public void anularCompra(int compraId);

	/** Las compras del usuario, de la mas reciente a la mas vieja. */
	public List<Compra> comprasDe(int usuarioId);

	/** Como registrarPago, pero rechaza la compra si no es de ese usuario. */
	public Compra registrarPagoDe(int usuarioId, int compraId);

	/** Como anularCompra, pero rechaza la compra si no es de ese usuario. */
	public void anularCompraDe(int usuarioId, int compraId);
}
