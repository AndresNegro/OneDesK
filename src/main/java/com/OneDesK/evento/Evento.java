package com.OneDesK.evento;


import com.OneDesK.modelo.Persistible;
import com.OneDesK.modelo.Planta;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
@Entity
@Table(name="Evento")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo")
public abstract class Evento extends Persistible {
	@ManyToOne
	@JoinColumn(name="ID_PLANTA", nullable = false)
    private Planta planta;

    protected Evento() {
    }

    protected Evento(Planta planta) {
        this.planta = planta;
    }

    public Planta getPlanta() { return planta; }

    /** REGADO, LUZ o VENTILADOR: el mismo valor que la columna tipo de la tabla Evento. */
    public abstract String getTipo();

    public abstract void setRealizado(boolean r);
    public abstract boolean getRealizado();

    @Override
    public String toString() {
        return "Evento{" + getClass().getSimpleName() + ", planta=" + planta.getGenetica() +
                ", realizado=" + getRealizado() + '}';
    }
}
