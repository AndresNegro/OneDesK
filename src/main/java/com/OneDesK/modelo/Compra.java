package com.OneDesK.modelo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.OneDesK.excepciones.OperacionInvalidaException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
	// vacia mientras la compra esta impaga: las compras con fecha de pago son los registros de pago
	@Column(name="fechaPago")
    private LocalDate fechaPago;
	// la pide el cliente y la acepta o rechaza el administrador
	@Enumerated(EnumType.STRING)
	@Column(name="estado")
    private EstadoCompra estado;
	// lo que eligio el cliente al pedirla: pagar apenas se apruebe o dejarla en cuenta corriente
	@Column(name="pagaAlAprobar")
    private boolean pagaAlAprobar;

	@ManyToOne
	@JoinColumn(name="ID_USUARIO", nullable = false)
    private Usuario usuario;

	@OneToMany(cascade= CascadeType.ALL, orphanRemoval = true )
	@JoinColumn(name = "ID_COMPRA", referencedColumnName= "ID", nullable = false)
    private List<ItemCompra> items;

	Compra(){
	}

    /** Una compra ya aprobada, pagada o no: la que se arma sin pasar por el administrador. */
    public Compra(LocalDate fechaCompra, boolean pagado, Usuario usuario) {
        this.fechaCompra = fechaCompra;
        this.pagado = pagado;
        this.usuario = usuario;
        this.items = new ArrayList<>();
        this.precio = 0;
        this.estado = EstadoCompra.APROBADA;
        this.pagaAlAprobar = pagado;
        // pagar al comprar es pagar ese mismo dia
        if (pagado) {
            this.fechaPago = fechaCompra;
        }
    }

    /**
     * La compra que pide un cliente: queda pendiente hasta que el administrador la acepte.
     * Mientras tanto no esta pagada ni suma deuda; lo que eligio el cliente se aplica al aprobarla.
     */
    public static Compra pedida(LocalDate fechaCompra, Usuario usuario, boolean pagaAlAprobar) {
        Compra compra = new Compra(fechaCompra, false, usuario);
        compra.estado = EstadoCompra.PENDIENTE;
        compra.pagaAlAprobar = pagaAlAprobar;
        return compra;
    }

    /** El administrador la acepta: si el cliente eligio pagar, queda pagada en ese momento. */
    public void aprobar() {
        verificarPendiente();
        this.estado = EstadoCompra.APROBADA;
        if (pagaAlAprobar) {
            this.pagado = true;
            this.fechaPago = LocalDate.now();
        }
    }

    /** El administrador la rechaza: queda en el historial como rechazada, sin cobro ni deuda. */
    public void rechazar() {
        verificarPendiente();
        this.estado = EstadoCompra.RECHAZADA;
    }

    private void verificarPendiente() {
        if (estado != EstadoCompra.PENDIENTE) {
            throw new OperacionInvalidaException("La compra ya fue " + (isAprobada() ? "aprobada" : "rechazada"));
        }
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
        if (!isAprobada()) {
            throw new OperacionInvalidaException("La compra todavía no fue aprobada");
        }
        this.pagado = true;
        this.fechaPago = LocalDate.now();
    }

    public LocalDate getFechaCompra() { return fechaCompra; }
    public LocalDate getFechaPago() { return fechaPago; }
    public boolean isPagado() { return pagado; }
    public EstadoCompra getEstado() { return estado; }
    public boolean isPendiente() { return estado == EstadoCompra.PENDIENTE; }
    public boolean isAprobada() { return estado == EstadoCompra.APROBADA; }
    public boolean isRechazada() { return estado == EstadoCompra.RECHAZADA; }
    public boolean isPagaAlAprobar() { return pagaAlAprobar; }

    /** Cuantos gramos tiene la compra, sumando todas sus geneticas. */
    public int getGramos() {
        int gramos = 0;
        for (ItemCompra item : items) {
            gramos += item.getCantidad();
        }
        return gramos;
    }
    public Usuario getUsuario() { return usuario; }
    public List<ItemCompra> getItems() { return Collections.unmodifiableList(items); }
    public int getPrecio() { return precio; }

    @Override
    public String toString() {
        return "Compra{fecha=" + fechaCompra + ", items=" + items.size() +
                ", total=$" + precio + ", pagado=" + pagado + '}';
    }
}
