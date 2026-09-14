package com.OneDesK.modelo;

import com.OneDesK.excepciones.StockInsuficienteException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name="Producto")
public class Producto extends Persistible{
	@Column(name="genetica", unique=true)
    private String genetica;
	@Column(name="stock")
    private int stock;
	@Column(name="precio")
    private int precio;

	Producto(){

	}


    public Producto(String genetica, int stock, int precio) {
        if (genetica == null || genetica.isBlank()) {
            throw new IllegalArgumentException("La genetica no puede estar vacia");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        if (precio <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        this.genetica = genetica.trim();
        this.stock = stock;
        this.precio = precio;
    }

    // sin setGenetica: la genetica es unica y es lo que usa la cosecha para encontrar el producto
    public String getGenetica() { return genetica; }
    // sin setStock: el stock solo cambia por cosechas (reponerStock) y por compras (descontarStock)
    public int getStock() { return stock; }
    public int getPrecio() { return precio; }
    public void setPrecio(int precio) {
        if (precio <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        this.precio = precio;
    }

    public void descontarStock(int cantidad) {
        validarCantidad(cantidad);
        if (cantidad > stock) {
            throw new StockInsuficienteException(
                "Stock insuficiente de " + genetica + ": hay " + stock + " y se piden " + cantidad);
        }
        this.stock -= cantidad;
    }

    public void reponerStock(int cantidad) {
        validarCantidad(cantidad);
        this.stock += cantidad;
    }

    // una cantidad negativa daria vuelta la operacion: reponer -50 descontaria y podria dejar stock negativo
    private void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
    }

    @Override
    public String toString() {
        return "Producto{" + genetica + ", stock=" + stock + ", $" + precio + '}';
    }
}
