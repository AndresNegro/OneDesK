package com.OneDesK.services;

import java.util.List;

import com.OneDesK.modelo.Compra;

public interface CompraService {

	public Compra realizarCompra(int usuarioId, List<LineaCompra> lineas, boolean pagado);

	public Compra registrarPago(int compraId);

	public void anularCompra(int compraId);
}
