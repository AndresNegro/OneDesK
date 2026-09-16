package com.OneDesK.modelo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.OneDesK.excepciones.OperacionInvalidaException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;


@Entity
@Table(name="Compra")
public class Compra extends Persistible{
	@Column(name="fechaCompra")
    private LocalDate fechaCompra;
	@Column(name="pagado")
    private boolean pagado;
	@Column(name="precio")
    private int precio;

	@ManyToOne
	@JoinColumn(name="ID_USUARIO", nullable = false)
    private Usuario usuario;

	@OneToMany(cascade= CascadeType.ALL, orphanRemoval = true )
	@JoinColumn(name = "ID_COMPRA", referencedColumnName= "ID", nullable = false)
    private List<ItemCompra> items;

	Compra(){
	}

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

    // sin setPagado: una compra pagada no vuelve a quedar impaga
    public void marcarComoPagada() {
        if (pagado) {
            throw new OperacionInvalidaException("La compra ya esta pagada");
        }
        this.pagado = true;
    }

    public LocalDate getFechaCompra() { return fechaCompra; }
    public boolean isPagado() { return pagado; }
    public Usuario getUsuario() { return usuario; }
    public List<ItemCompra> getItems() { return Collections.unmodifiableList(items); }
    public int getPrecio() { return precio; }

    @Override
    public String toString() {
        return "Compra{fecha=" + fechaCompra + ", items=" + items.size() +
                ", total=$" + precio + ", pagado=" + pagado + '}';
    }
}
