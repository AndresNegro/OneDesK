package com.OneDesK.modelo;

import com.OneDesK.evento.Evento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Indoor {
    private final List<Planta> plantas;
    private final List<Evento> colaEventos;

    public Indoor() {
        this.plantas = new ArrayList<>();
        this.colaEventos = new ArrayList<>();
    }

    public Planta addPlanta(Planta p) {
        plantas.add(p);
        p.setIndoor(this);
        return p;
    }

    public void deletePlanta(Planta p) {
        plantas.remove(p);
        if (p.getIndoor() == this) p.setIndoor(null);
    }

    public List<Planta> getPlantas() { return Collections.unmodifiableList(plantas); }

    // Cola de eventos hacia el empleado a cargo (wait/notifyAll):
    // las Plantas producen, el menú del empleado consume (atender).
    public synchronized void recibirEvento(Evento e) {
        colaEventos.add(e);
        notifyAll();
    }

    public synchronized List<Evento> eventosEnCola() {
        return new ArrayList<>(colaEventos);
    }

    public synchronized Evento consumirEvento(int index) {
        if (index < 0 || index >= colaEventos.size()) return null;
        return colaEventos.remove(index);
    }

    public synchronized int colaSize() { return colaEventos.size(); }

    @Override
    public String toString() {
        return "Indoor{plantas=" + plantas.size() + ", cola=" + colaEventos.size() + '}';
    }
}
