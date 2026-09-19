package com.OneDesK.evento;
import com.OneDesK.modelo.Planta;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;


@Entity
@DiscriminatorValue("REGADO")
public class EventoRegado extends Evento {

	@Column(name="realizado")
    private boolean regado;

    EventoRegado() {
    }

    public EventoRegado(Planta planta) {
        super(planta);
        this.regado = false;
    }

    @Override
    public String getTipo() { return "REGADO"; }

    @Override
    public synchronized void setRealizado(boolean r) { this.regado = r; }

    @Override
    public synchronized boolean getRealizado() { return regado; }

    @Override
    public String toString() {
        return "EventoRegado{planta=" + getPlanta().getGenetica() + ", regado=" + regado + '}';
    }
}
