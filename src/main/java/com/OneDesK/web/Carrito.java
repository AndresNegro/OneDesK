package com.OneDesK.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.OneDesK.services.LineaCompra;

/**
 * Lo que el usuario va eligiendo antes de comprar. Vive en la sesion y no en la base: recien al
 * confirmar se convierte en una Compra, y ahi CompraService valida stock, precios y tope.
 */
public class Carrito implements Serializable {

	private static final long serialVersionUID = 1L;

	private final List<Linea> lineas = new ArrayList<>();

	/** Suma la cantidad a la linea del producto, o crea la linea si todavia no estaba. */
	public void agregar(int productoId, int cantidad) {
		validarCantidad(cantidad);
		Linea linea = buscar(productoId);
		if (linea == null) {
			lineas.add(new Linea(productoId, cantidad));
		} else {
			linea.cantidad += cantidad;
		}
	}

	public void cambiarCantidad(int productoId, int cantidad) {
		validarCantidad(cantidad);
		Linea linea = buscar(productoId);
		if (linea != null) {
			linea.cantidad = cantidad;
		}
	}

	public void quitar(int productoId) {
		lineas.removeIf(linea -> linea.productoId == productoId);
	}

	public void vaciar() {
		lineas.clear();
	}

	public boolean estaVacio() {
		return lineas.isEmpty();
	}

	/** Cuantas unidades hay en total, para el numero del menu. */
	public int unidades() {
		int total = 0;
		for (Linea linea : lineas) {
			total += linea.cantidad;
		}
		return total;
	}

	public List<Linea> getLineas() {
		return Collections.unmodifiableList(lineas);
	}

	/** Las lineas en el formato que pide CompraService. */
	public List<LineaCompra> comoLineasDeCompra() {
		List<LineaCompra> resultado = new ArrayList<>();
		for (Linea linea : lineas) {
			resultado.add(new LineaCompra(linea.productoId, linea.cantidad));
		}
		return resultado;
	}

	private Linea buscar(int productoId) {
		for (Linea linea : lineas) {
			if (linea.productoId == productoId) {
				return linea;
			}
		}
		return null;
	}

	private void validarCantidad(int cantidad) {
		if (cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad tiene que ser mayor a cero");
		}
	}

	/** Un producto del carrito con la cantidad elegida. */
	public static class Linea implements Serializable {

		private static final long serialVersionUID = 1L;

		private final int productoId;
		private int cantidad;

		Linea(int productoId, int cantidad) {
			this.productoId = productoId;
			this.cantidad = cantidad;
		}

		public int getProductoId() { return productoId; }
		public int getCantidad() { return cantidad; }
	}
}
