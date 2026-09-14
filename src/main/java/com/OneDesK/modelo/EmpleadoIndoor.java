package com.OneDesK.modelo;


import com.OneDesK.evento.*;
import com.OneDesK.excepciones.OperacionInvalidaException;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Entity
@Table(name ="EmpleadoIndoor")
public class EmpleadoIndoor extends Empleado {
	@ManyToMany
	@JoinTable(name = "Trabaja",
		joinColumns = @JoinColumn(name = "ID_EMPLEADO_INDOOR"),
		inverseJoinColumns = @JoinColumn(name = "ID_INDOOR"))
    private List<Indoor> sectoresACargo;
	@Column(name ="salarioMensual")
    private int salarioMensual;

	EmpleadoIndoor(){
		super();
	}

    public EmpleadoIndoor(String nombre, String apellido, String email, String contrasenia, int salarioMensual) {
        super(nombre, apellido, email, contrasenia);
        this.sectoresACargo = new ArrayList<>();
        setSalarioMensual(salarioMensual);
    }

    public boolean estaAsignadoA(Indoor indoor) {
        return sectoresACargo.contains(indoor);
    }

    public void atenderEvento(Evento e) {
        if (!estaAsignadoA(e.getPlanta().getIndoor())) {
            throw new OperacionInvalidaException(
                    "El empleado " + getNombre() + " no esta asignado al indoor de ese evento");
        }
        if (e.getRealizado()) {
            throw new OperacionInvalidaException("El evento ya fue atendido");
        }
        e.setRealizado(true);
        aplicarEfectoEnPlanta(e);
    }

    private void aplicarEfectoEnPlanta(Evento e) {
        Planta p = e.getPlanta();
        if (e instanceof EventoRegado) {
            p.regar();
        } else if (e instanceof EventoLuz) {
            p.setLuz(!p.isLuz());
        } else if (e instanceof EventoVentilador) {
            p.setVentilador(!p.isVentilador());
        }
    }

    public void addIndoor(Indoor i) {
        if (estaAsignadoA(i)) {
            throw new OperacionInvalidaException("El empleado " + getNombre() + " ya esta asignado a ese indoor");
        }
        sectoresACargo.add(i);
    }

    public void deleteIndoor(Indoor i) {
        if (!sectoresACargo.remove(i)) {
            throw new OperacionInvalidaException("El empleado " + getNombre() + " no esta asignado a ese indoor");
        }
    }

    public List<Indoor> getSectoresACargo() { return Collections.unmodifiableList(sectoresACargo); }

    public void setSalarioMensual(int sm) {
        if (sm <= 0) {
            throw new IllegalArgumentException("El salario mensual debe ser mayor a cero");
        }
        this.salarioMensual = sm;
    }

    public int getSalarioMensual() { return salarioMensual; }

    /** Los eventos sin atender de todos los indoors a cargo del empleado. */
    public List<Evento> eventosPendientes() {
        List<Evento> pendientes = new ArrayList<>();
        for (Indoor indoor : sectoresACargo) {
            pendientes.addAll(indoor.getEventosPendientes());
        }
        return pendientes;
    }

    @Override
    public String toString() {
        return "EmpleadoIndoor{" + getNombre() + " " + getApellido() +
                ", sectores=" + sectoresACargo.size() + '}';
    }
}
