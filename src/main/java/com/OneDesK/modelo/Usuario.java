package com.OneDesK.modelo;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
@Entity
@Table(name="Usuario")
public class Usuario extends Persona {
	@OneToMany (cascade= CascadeType.ALL, orphanRemoval = true )
	@JoinColumn(name="ID_USUARIO",referencedColumnName="ID")
    private final List<Compra> compras;
	@OneToOne(cascade=CascadeType.ALL,  orphanRemoval=true)
	@JoinColumn(name="ID_DEUDA")
    private final Deuda deuda;

    public Usuario(String nombre, String apellido, String email, String contrasenia) {
        super(nombre, apellido, email, contrasenia);
        this.compras = new ArrayList<>();
        this.deuda = new Deuda();
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

    public void realizarCompra(Compra c) {
        if (c == null) return;
        for (ItemCompra it : c.getItems()) {
            Producto p = it.getProducto();
            p.setStock(p.getStock() - it.getCantidad());
        }
        compras.add(c);
        if (!c.isPagado()) {
            deuda.addCompra(c);
        }
    }

    public void registrarPago(Compra c) {
        if (c == null || c.isPagado()) return;
        c.setPagado(true);
        deuda.deleteCompra(c);
    }

    public List<Compra> getCompras() { return compras; }
    public Deuda getDeuda() { return deuda; }
    public void deleteCompra(Compra c) { compras.remove(c); }
}
