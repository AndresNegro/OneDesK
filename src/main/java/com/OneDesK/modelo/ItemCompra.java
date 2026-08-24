package com.OneDesK.modelo;

public class ItemCompra {
    private Producto producto;
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
