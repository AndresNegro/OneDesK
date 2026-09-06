package com.OneDesK.modelo;


import com.OneDesK.evento.*;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Entity
@Table(name ="EmpleadoIndoor")
public class EmpleadoIndoor extends Empleado {
	@CollectionTable(name= "Trabaja", joinColumns =	@JoinColumn(name ="ID_EMPLEADO_INDOOR", referencedColumnName="ID"))
	@Column(name ="ID_INDOOR")
    private final List<Indoor> sectoresACargo;
	@Column(name ="salarioMensual")
    private int salarioMensual;

    public EmpleadoIndoor(String nombre, String apellido, String email, String contrasenia, int salarioMensual) {
        super(nombre, apellido, email, contrasenia);
        this.sectoresACargo = new ArrayList<>();
        this.salarioMensual = salarioMensual;
    }

    public void cargarRegistroProduccion(Indoor indoor, Producto p, int cantidad) {
        RegistroProduccion r = new RegistroProduccion(indoor, this, p, cantidad);
        System.out.println("  Registro de produccion cargado: " + r);
        modificarProducto(p,p.getStock()+cantidad,null);
    }

    // Alta de producto: agrega un nuevo Producto a la lista recibida.
    public void agregarProducto(List<Producto> lista, Producto p) {
        if (p == null) return;
        if (!lista.contains(p)) {
            lista.add(p);
        }
    }

    // Modificacion: actualiza stock y/o precio de un producto existente.
    public void modificarProducto(Producto p, Integer nuevoStock, Integer nuevoPrecio) {
        if (p == null) return;
        if (nuevoStock != null) p.setStock(nuevoStock);
        if (nuevoPrecio != null) p.setPrecio(nuevoPrecio);
    }

    public void atenderEvento(Evento e) {
        if (e == null) return;
        e.setRealizado(true);
        aplicarEfectoEnPlanta(e);
    }

    private void aplicarEfectoEnPlanta(Evento e) {
        Planta p = e.getPlanta();
        if (e instanceof EventoRegado) {
            p.regar();
        } else if (e instanceof EventoLuz) {
            p.setLuz(!p.isLuz());
        } else if (e instanceof EventoVentilador) {
            p.setVentilador(!p.isVentilador());
        }
    }

    public void addIndoor(Indoor i) { sectoresACargo.add(i); }
    public void deleteIndoor(Indoor i) { sectoresACargo.remove(i); }
    public List<Indoor> getSectoresACargo() { return Collections.unmodifiableList(sectoresACargo); }
    public void setSalarioMensual(int sm) { this.salarioMensual = sm; }
    public int getSalarioMensual() { return salarioMensual; }

    public List<Evento> eventosPendientesDeIndoor() {
        List<Evento> pendientes = new ArrayList<>();
        for (Indoor in : sectoresACargo) pendientes.addAll(in.getColaEventos());
        return pendientes;
    }

    public Evento tomarEventoPendiente(int index) {
        int offset = index;
        for (Indoor in : sectoresACargo) {
            int n = in.colaSize();
            if (offset < n) return in.consumirEvento(offset);
            offset -= n;
        }
        return null;
    }

    @Override
    public String toString() {
        return "EmpleadoIndoor{" + getNombre() + " " + getApellido() +
                ", sectores=" + sectoresACargo.size();
    }
}
