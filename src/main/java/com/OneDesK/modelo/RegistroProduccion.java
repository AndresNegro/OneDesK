package com.OneDesK.modelo;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Sin setters: un registro de produccion es historico y no se modifica una vez cargado
@Entity
@Table(name="RegistroProduccion")
public class RegistroProduccion extends Persistible {
	@ManyToOne
	@JoinColumn(name="ID_PLANTA", nullable = false)
    private Planta planta;
	@ManyToOne
	@JoinColumn(name="ID_INDOOR", nullable = false)
    private Indoor indoor;
	@ManyToOne
	@JoinColumn(name="ID_EMPLEADO_INDOOR", nullable = false)
    private EmpleadoIndoor empleado;
	@ManyToOne
	@JoinColumn(name="ID_PRODUCTO", nullable = false)
    private Producto producto;
	@Column(name="cantidad")
    private int cantidad;
	@Column(name="fechaRegistro")
    private LocalDate fechaRegistro;

	RegistroProduccion(){

	}

    /** El indoor se toma de la planta, asi el registro no puede apuntar a un indoor distinto del de la planta. */
    public RegistroProduccion(Planta planta, EmpleadoIndoor empleado, Producto producto, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad cosechada debe ser mayor a cero");
        }
        this.planta = planta;
        this.indoor = planta.getIndoor();
        this.empleado = empleado;
        this.producto = producto;
        this.cantidad = cantidad;
        this.fechaRegistro = LocalDate.now();
    }

    public Planta getPlanta() { return planta; }
    public Indoor getIndoor() { return indoor; }
    public EmpleadoIndoor getEmpleadoIndoor() { return empleado; }
    public Producto getProducto() { return producto; }
    public int getCantidad() { return cantidad; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }

    @Override
    public String toString() {
        return "RegistroProduccion{" + producto + " x" + cantidad + ", fecha=" + fechaRegistro +
                ", empleado=" + (empleado == null ? "null" : empleado.getNombre()) + '}';
    }
}
