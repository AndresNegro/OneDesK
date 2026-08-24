package com.OneDesK.modelo;


import com.OneDesK.evento.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EmpleadoIndoor extends Empleado {
    private final List<Indoor> sectoresACargo;
    private int salarioMensual;
    private final List<Evento> eventos;

    public EmpleadoIndoor(String nombre, String apellido, String email, String contrasenia, int salarioMensual) {
        super(nombre, apellido, email, contrasenia);
        this.sectoresACargo = new ArrayList<>();
        this.salarioMensual = salarioMensual;
        this.eventos = new ArrayList<>();
    }

    public void cargarRegistroProduccion(Indoor indoor, String genetica, int cantidad) {
        RegistroProduccion r = new RegistroProduccion(indoor, this, genetica, cantidad);
        System.out.println("  Registro de produccion cargado: " + r);
    }

    public RegistroProduccion armarRegistroProduccion(Indoor indoor, String genetica, int cantidad) {
        return new RegistroProduccion(indoor, this, genetica, cantidad);
    }

    // Alta de producto: agrega un nuevo Producto a la lista recibida.
    public void agregarProducto(List<Producto> lista, Producto p) {
        if (p == null) return;
        lista.add(p);
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
        synchronized (eventos) {
            eventos.add(e);
        }
        e.getPlanta().notificarAtendido();
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
    public List<Evento> getEventos() {
        synchronized (eventos) {
            return new ArrayList<>(eventos);
        }
    }
    public List<Evento> eventosAtendidos() {
        synchronized (eventos) {
            List<Evento> hechos = new ArrayList<>();
            for (Evento e : eventos) if (e.getRealizado()) hechos.add(e);
            return hechos;
        }
    }

    public List<Evento> eventosPendientesDeIndoor() {
        List<Evento> pendientes = new ArrayList<>();
        for (Indoor in : sectoresACargo) pendientes.addAll(in.eventosEnCola());
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
                ", sectores=" + sectoresACargo.size() + ", atendidos=" + eventos.size() + '}';
    }
}
