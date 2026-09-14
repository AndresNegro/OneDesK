package com.OneDesK.modelo;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
@Entity
@Table(name="Usuario")
public class Usuario extends Persona {
	@OneToMany (mappedBy="usuario", cascade= CascadeType.ALL, orphanRemoval = true )
    private List<Compra> compras;
	@OneToOne(cascade=CascadeType.ALL,  orphanRemoval=true)
	@JoinColumn(name="ID_DEUDA")
    private Deuda deuda;
	@Column(name="topeCredito")
    private int topeCredito;

	Usuario(){
	}
	
    public Usuario(String nombre, String apellido, String email, String contrasenia) {
        super(nombre, apellido, email, contrasenia);
        this.compras = new ArrayList<>();
        this.deuda = new Deuda();
        this.topeCredito = 0;
    }

    public void verProductos(List<Producto> productos) {
        if (productos.isEmpty()) {
            System.out.println("  No hay productos cargados.");
            return;
        }
        for (Producto p : productos) {
            System.out.println("  - " + p.getGenetica() + " | stock=" + p.getStock() + " | $" + p.getPrecio());
        }
    }

    public List<Producto> buscarPorGenetica(String query, List<Producto> productos) {
        List<Producto> resultado = new ArrayList<>();
        if (query == null) return resultado;
        String q = query.toLowerCase();
        for (Producto p : productos) {
            if (p.getGenetica().toLowerCase().contains(q)) resultado.add(p);
        }
        return resultado;
    }

    public List<Producto> ordenarPorPrecio(List<Producto> productos) {
        List<Producto> copia = new ArrayList<>(productos);
        copia.sort(Comparator.comparingInt(Producto::getPrecio));
        return copia;
    }

    public void agregarCompra(Compra c) {
        if (c == null) return;
        compras.add(c);
        recalcularDeuda();
    }

    public void registrarPago(Compra c) {
        if (c == null || c.isPagado()) return;
        c.setPagado(true);
        recalcularDeuda();
    }

    public void recalcularDeuda() {
        int total = 0;
        for (Compra c : compras) {
            if (!c.isPagado()) total += c.getPrecio();
        }
        deuda.setMonto(total);
    }

    public List<Compra> comprasImpagas() {
        List<Compra> impagas = new ArrayList<>();
        for (Compra c : compras) {
            if (!c.isPagado()) impagas.add(c);
        }
        return impagas;
    }

    public List<Compra> getCompras() { return compras; }
    public Deuda getDeuda() { return deuda; }
    public int getTopeCredito() { return topeCredito; }
    public void setTopeCredito(int topeCredito) { this.topeCredito = topeCredito; }
    public void deleteCompra(Compra c) {
        if (compras.remove(c)) recalcularDeuda();
    }
}
