package com.OneDesK.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deuda {
    private int monto;
    private final List<Compra> compras;

    public Deuda() {
        this.monto = 0;
        this.compras = new ArrayList<>();
    }

    public void addCompra(Compra c) {
        if (c == null || c.isPagado()) return;
        if (compras.contains(c)) return;
        compras.add(c);
        monto += c.getPrecio();
    }

    public void deleteCompra(Compra c) {
        if (compras.remove(c)) {
            monto -= c.getPrecio();
            if (monto < 0) monto = 0;
        }
    }

    public void setMonto(int montoActualizado) { this.monto = montoActualizado; }
    public int getMonto() { return monto; }
    public List<Compra> getCompras() { return Collections.unmodifiableList(compras); }

    @Override
    public String toString() {
        return "Deuda{monto=$" + monto + ", compras=" + compras.size() + '}';
    }
}
