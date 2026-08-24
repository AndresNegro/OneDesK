package com.OneDesK.modelo;

public class Producto {
    private String genetica;
    private int stock;
    private int precio;

    public Producto(String genetica, int stock, int precio) {
        this.genetica = genetica;
        this.stock = stock;
        this.precio = precio;
    }

    public String getGenetica() { return genetica; }
    public void setGenetica(String g) { this.genetica = g; }
    public int getStock() { return stock; }
    public void setStock(int s) { this.stock = s; }
    public int getPrecio() { return precio; }
    public void setPrecio(int precio) { this.precio = precio; }

    @Override
    public String toString() {
        return "Producto{" + genetica + ", stock=" + stock + ", $" + precio + '}';
    }
}
