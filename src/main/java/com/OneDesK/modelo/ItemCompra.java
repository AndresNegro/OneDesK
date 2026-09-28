package com.OneDesK.modelo;

import com.OneDesK.excepciones.DatoInvalidoException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Sin setters: la compra suma el precio del item al agregarlo, cambiarlo despues dejaria mal el total
@Entity
@Table(name="ItemCompra")
public class ItemCompra extends Persistible {

	@ManyToOne
	@JoinColumn(name="ID_PRODUCTO", nullable = false)
    private Producto producto;
	@Column(name="cantidad")
    private int cantidad;
	@Column(name="precioUnitario")
    private int precioUnitario;

	ItemCompra(){

	}

    public ItemCompra(Producto producto, int cantidad) {
        if (cantidad <= 0) {
            throw new DatoInvalidoException("error.item.cantidad");
        }
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecio();
    }

    public Producto getProducto() { return producto; }
    public int getCantidad() { return cantidad; }

    /** Le devuelve al catalogo los gramos que este item tenia reservados. */
    void devolverStock() {
        producto.reponerStock(cantidad);
    }

    public int getPrecioUnitario() { return precioUnitario; }

    public int getPrecio() {
        return precioUnitario * cantidad;
    }

    @Override
    public String toString() {
        return "ItemCompra{" + producto.getGenetica() + " x" + cantidad + " = $" + getPrecio() + '}';
    }
}
