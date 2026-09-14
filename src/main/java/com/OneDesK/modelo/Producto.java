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

    public String getGenetica() { return genetica; }
    public void setGenetica(String g) { this.genetica = g; }
    public int getStock() { return stock; }
    public void setStock(int s) { this.stock = s; }
    public int getPrecio() { return precio; }
    public void setPrecio(int precio) { this.precio = precio; }

    public void descontarStock(int cantidad) {
        if (cantidad > stock) {
            throw new StockInsuficienteException(
                "Stock insuficiente de " + genetica + ": hay " + stock + " y se piden " + cantidad);
        }
        this.stock -= cantidad;
    }

    public void reponerStock(int cantidad) {
        this.stock += cantidad;
    }

    @Override
    public String toString() {
        return "Producto{" + genetica + ", stock=" + stock + ", $" + precio + '}';
    }
}
