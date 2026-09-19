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
	// al registrarse queda pendiente: recien puede ingresar y comprar cuando el administrador lo aprueba
	@Column(name="aprobado")
    private boolean aprobado;

	Usuario(){
	}

    public Usuario(String nombre, String apellido, String email, String contrasenia) {
        super(nombre, apellido, email, contrasenia);
        this.compras = new ArrayList<>();
        this.deuda = new Deuda();
        this.topeCredito = 0;
        this.aprobado = false;
    }

    public boolean isAprobado() { return aprobado; }

    /** El administrador acepta la solicitud de registro y le asigna el tope de credito en el mismo paso. */
    public void aprobar(int topeCredito) {
        if (aprobado) {
            throw new OperacionInvalidaException("El usuario " + getEmail() + " ya esta aprobado");
        }
        // el tope se valida antes de aprobar: si es invalido, el usuario sigue pendiente
        setTopeCredito(topeCredito);
        this.aprobado = true;
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
        // una compra pendiente o rechazada todavia no se debe
        for (Compra c : compras) {
            if (c.isAprobada() && !c.isPagado()) total += c.getPrecio();
        }
        deuda.setMonto(total);
    }

    /** Lo que ya debe mas lo que pidio en cuenta corriente y espera aprobacion: es lo que se compara con el tope. */
    public int deudaComprometida() {
        recalcularDeuda();
        int total = deuda.getMonto();
        for (Compra c : compras) {
            if (c.isPendiente() && !c.isPagaAlAprobar()) total += c.getPrecio();
        }
        return total;
    }

    public List<Compra> comprasImpagas() {
        List<Compra> impagas = new ArrayList<>();
        for (Compra c : compras) {
            if (c.isAprobada() && !c.isPagado()) impagas.add(c);
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
