package com.OneDesK.evento;

import com.OneDesK.modelo.Planta;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("LUZ")
public class EventoLuz extends Evento {
	@Column(name="realizado")
    private boolean realizado;

    EventoLuz() {
    }

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
