package com.OneDesK.evento;
import com.OneDesK.modelo.Planta;
public class EventoRegado extends Evento {
    private boolean regado;

    public EventoRegado(Planta planta) {
        super(planta);
        this.regado = false;
    }

    @Override
    public synchronized void setRealizado(boolean r) { this.regado = r; }

    @Override
    public synchronized boolean getRealizado() { return regado; }

    public boolean isRegado() { return regado; }
    public void setRegado(boolean regado) { this.regado = regado; }

    @Override
    public String toString() {
        return "EventoRegado{planta=" + getPlanta().getGenetica() + ", regado=" + regado + '}';
    }
}
