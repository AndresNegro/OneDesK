package com.OneDesK.evento;

import com.OneDesK.modelo.Planta;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("VENTILADOR")
public class EventoVentilador extends Evento {
	@Column(name="realizado")
    private boolean realizado;

    EventoVentilador() {
    }

    public EventoVentilador(Planta planta) {
        super(planta);
        this.realizado = false;
    }

    @Override
    public String getTipo() { return "VENTILADOR"; }

    @Override
    public synchronized void setRealizado(boolean r) { this.realizado = r; }

    @Override
    public synchronized boolean getRealizado() { return realizado; }

    @Override
    public String toString() {
        return "EventoVentilador{planta=" + getPlanta().getGenetica() + ", realizado=" + realizado + '}';
    }
}
