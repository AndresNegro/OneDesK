package com.OneDesK.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="ItemCompra")
public class ItemCompra extends Persistible {

	@ManyToOne
	@JoinColumn(name="ID_PRODUCTO")
    private Producto producto;
	@Column(name="cantidad")
    private int cantidad;
	@Column(name="precioUnitario")
    private int precioUnitario;

	ItemCompra(){

	}

    public ItemCompra(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecio();
    }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto p) { this.producto = p; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cant) { this.cantidad = cant; }

    public int getPrecioUnitario() { return precioUnitario; }

    public int getPrecio() {
        return precioUnitario * cantidad;
    }

    @Override
    public String toString() {
        return "ItemCompra{" + producto.getGenetica() + " x" + cantidad + " = $" + getPrecio() + '}';
    }
}
