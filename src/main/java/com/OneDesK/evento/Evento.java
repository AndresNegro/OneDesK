package com.OneDesK.evento;


import com.OneDesK.modelo.Planta;

public abstract class Evento {
    private Planta planta;

    protected Evento(Planta planta) {
        this.planta = planta;
    }

    public void setPlanta(Planta p) { this.planta = p; }
    public Planta getPlanta() { return planta; }

    public abstract void setRealizado(boolean r);
    public abstract boolean getRealizado();

    @Override
    public String toString() {
        return "Evento{" + getClass().getSimpleName() + ", planta=" + planta.getGenetica() +
                ", realizado=" + getRealizado() + '}';
    }
}
