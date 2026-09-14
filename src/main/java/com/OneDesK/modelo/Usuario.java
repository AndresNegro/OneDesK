package com.OneDesK.modelo;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.OneDesK.excepciones.OperacionInvalidaException;

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

    public void agregarCompra(Compra c) {
        if (c.getUsuario() != this) {
            throw new OperacionInvalidaException("La compra pertenece a otro usuario");
        }
        if (compras.contains(c)) {
            throw new OperacionInvalidaException("La compra ya fue agregada");
        }
        compras.add(c);
        recalcularDeuda();
    }

    public void registrarPago(Compra c) {
        verificarQueEsSuya(c);
        c.marcarComoPagada();
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

    // no modificable: agregar o sacar compras tiene que pasar por los metodos que recalculan la deuda
    public List<Compra> getCompras() { return Collections.unmodifiableList(compras); }
    public Deuda getDeuda() { return deuda; }
    public int getTopeCredito() { return topeCredito; }
    public void setTopeCredito(int topeCredito) {
        if (topeCredito < 0) {
            throw new IllegalArgumentException("El tope de credito no puede ser negativo");
        }
        this.topeCredito = topeCredito;
    }

    public void deleteCompra(Compra c) {
        verificarQueEsSuya(c);
        compras.remove(c);
        recalcularDeuda();
    }

    private void verificarQueEsSuya(Compra c) {
        if (!compras.contains(c)) {
            throw new OperacionInvalidaException("La compra no pertenece a este usuario");
        }
    }
}
