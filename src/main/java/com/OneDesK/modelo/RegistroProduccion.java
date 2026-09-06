package com.OneDesK.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="RegistroProduccion")
public class RegistroProduccion extends Persistible {
	@OneToOne(cascade= CascadeType.ALL, orphanRemoval = true )
	@JoinColumn(name="ID_INDOOR")
    private Indoor indoor;
	@OneToOne(cascade= CascadeType.ALL, orphanRemoval = true )
	@JoinColumn(name="ID_EMPLEADO_INDOOR")
    private EmpleadoIndoor empleado;
	@OneToOne(cascade= CascadeType.ALL, orphanRemoval = true )
	@JoinColumn(name="ID_PRODUCTO")
    private Producto producto;
	@Column(name="cantidad")
    private int cantidad;

    public RegistroProduccion(Indoor indoor, EmpleadoIndoor empleado, Producto producto, int cantidad) {
        this.indoor = indoor;
        this.empleado = empleado;
        this.producto = producto;
        this.cantidad = cantidad;
    }

    public void setIndoor(Indoor i) { this.indoor = i; }
    public Indoor getIndoor() { return indoor; }
    public void setEmpleado(EmpleadoIndoor ei) { this.empleado = ei; }
    public EmpleadoIndoor getEmpleadoIndoor() { return empleado; }
    public void setProducto(Producto p) { this.producto = p; }
    public Producto getProducto() { return producto; }
    public void setCantidad(int cant) { this.cantidad = cant; }
    public int getCantidad() { return cantidad; }

    @Override
    public String toString() {
        return "RegistroProduccion{" + producto + " x" + cantidad +
                ", indoor=" + indoor + ", empleado=" + (empleado == null ? "null" : empleado.getNombre()) + '}';
    }
}
