package com.OneDesK.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name="Deuda")
public class Deuda extends Persistible{
	@Column(name="monto")
    private int monto;

    public Deuda() {
        this.monto = 0;
    }

    // sin public: el monto se deriva de las compras impagas y solo lo actualiza Usuario
    void setMonto(int montoActualizado) { this.monto = montoActualizado; }
    public int getMonto() { return monto; }

    @Override
    public String toString() {
        return "Deuda{monto=$" + monto + '}';
    }
}
