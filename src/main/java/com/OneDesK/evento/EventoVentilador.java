package com.OneDesK.evento;

import com.OneDesK.modelo.Planta;

public class EventoVentilador extends Evento {
    private boolean realizado;

    public EventoVentilador(Planta planta) {
        super(planta);
        this.realizado = false;
    }

    @Override
    public synchronized void setRealizado(boolean r) { this.realizado = r; }

    @Override
    public synchronized boolean getRealizado() { return realizado; }

    @Override
    public String toString() {
        return "EventoVentilador{planta=" + getPlanta().getGenetica() + ", realizado=" + realizado + '}';
    }
}
