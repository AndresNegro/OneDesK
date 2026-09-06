package com.OneDesK.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="ItemCompra")
public class ItemCompra extends Persistible {

	@OneToOne (cascade= CascadeType.ALL, orphanRemoval = true )
	@JoinColumn(name="ID_PRODUCTO")
    private Producto producto;
	@Column(name="cantidad")
    private int cantidad;

    public ItemCompra(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
    }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto p) { this.producto = p; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cant) { this.cantidad = cant; }

    public int getPrecio() {
        return producto.getPrecio() * cantidad;
    }

    @Override
    public String toString() {
        return "ItemCompra{" + producto.getGenetica() + " x" + cantidad + " = $" + getPrecio() + '}';
    }
}
