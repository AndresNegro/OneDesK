package com.OneDesK.modelo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Compra {
    private final LocalDate fechaCompra;
    private boolean pagado;
    private int precio;
    private final Usuario usuario;
    private final List<ItemCompra> items;

    public Compra(LocalDate fechaCompra, boolean pagado, Usuario usuario) {
        this.fechaCompra = fechaCompra;
        this.pagado = pagado;
        this.usuario = usuario;
        this.items = new ArrayList<>();
        this.precio = 0;
    }

    public void addItem(ItemCompra i) {
        items.add(i);
        precio += i.getPrecio();
    }

    public void deleteItem(ItemCompra ic) {
        if (items.remove(ic)) {
            precio -= ic.getPrecio();
            if (precio < 0) precio = 0;
        }
    }

    public LocalDate getFechaCompra() { return fechaCompra; }
    public boolean isPagado() { return pagado; }
    public void setPagado(boolean aux) { this.pagado = aux; }
    public Usuario getUsuario() { return usuario; }
    public List<ItemCompra> getItems() { return Collections.unmodifiableList(items); }
    public int getPrecio() { return precio; }

    @Override
    public String toString() {
        return "Compra{fecha=" + fechaCompra + ", items=" + items.size() +
                ", total=$" + precio + ", pagado=" + pagado + '}';
    }
}
