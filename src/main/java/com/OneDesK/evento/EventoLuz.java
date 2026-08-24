package com.OneDesK.evento;

import com.OneDesK.modelo.Planta;

public class EventoLuz extends Evento {
    private boolean realizado;

    public EventoLuz(Planta planta) {
        super(planta);
        this.realizado = false;
    }

    @Override
    public synchronized void setRealizado(boolean r) { this.realizado = r; }

    @Override
    public synchronized boolean getRealizado() { return realizado; }

    @Override
    public String toString() {
        return "EventoLuz{planta=" + getPlanta().getGenetica() + ", realizado=" + realizado + '}';
    }
}
