package com.OneDesK.modelo;

import com.OneDesK.evento.Evento;
import com.OneDesK.excepciones.OperacionInvalidaException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name="Indoor")
public class Indoor extends Persistible{
	@Column(name="nombre", unique=true)
	private String nombre;
	// cuantas plantas en cultivo entran a la vez: las cosechadas ya no ocupan lugar
	@Column(name="capacidad")
	private int capacidad;
	// lado inverso: las asignaciones se hacen desde EmpleadoIndoor, que es quien escribe en Trabaja
	@ManyToMany(mappedBy = "sectoresACargo")
	private List<EmpleadoIndoor> empleadosAsignados;
	@OneToMany(mappedBy = "indoor", cascade= CascadeType.ALL, orphanRemoval = true )
    private List<Planta> plantas;
	@OneToMany(cascade= CascadeType.ALL, orphanRemoval = true )
	@JoinColumn(name = "ID_INDOOR", referencedColumnName="ID", nullable = false)
    private List<Evento> colaEventos;

    Indoor() {
    	this.empleadosAsignados = new ArrayList<>();
        this.plantas = new ArrayList<>();
        this.colaEventos = new ArrayList<>();
    }

    public Indoor(String nombre, int capacidad) {
        this();
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del indoor no puede estar vacío");
        }
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad del indoor tiene que ser de al menos una planta");
        }
        this.nombre = nombre.trim();
        this.capacidad = capacidad;
    }

    public String getNombre() { return nombre; }
    public int getCapacidad() { return capacidad; }

    /** Las plantas que todavia no se cosecharon: son las que ocupan lugar. */
    public int plantasEnCultivo() {
        int enCultivo = 0;
        for (Planta planta : plantas) {
            if (!planta.isCosechada()) {
                enCultivo++;
            }
        }
        return enCultivo;
    }

    public boolean isLleno() {
        return plantasEnCultivo() >= capacidad;
    }

    public Planta addPlanta(Planta p) {
        if (isLleno()) {
            throw new OperacionInvalidaException("El indoor " + nombre + " está lleno: tiene " + plantasEnCultivo()
                    + " de " + capacidad + " plantas en cultivo");
        }
        plantas.add(p);
        p.setIndoor(this);
        p.empezarACorrerTiempos(LocalDateTime.now());
        return p;
    }

    /** Quita una planta no cosechada junto con todos sus eventos, que sin la planta no tienen sentido. */
    public void deletePlanta(Planta p) {
        if (p.isCosechada()) {
            throw new OperacionInvalidaException("No se puede quitar la planta " + p.getGenetica()
                    + ": ya fue cosechada y su registro de produccion la necesita");
        }
        if (plantas.remove(p)) {
            colaEventos.removeIf(evento -> evento.getPlanta() == p);
            p.setIndoor(null);
        }
    }

    public Planta buscarPlanta(int plantaId) {
        for (Planta planta : plantas) {
            if (planta.getId() == plantaId) {
                return planta;
            }
        }
        return null;
    }

    public List<Planta> getPlantas() { return Collections.unmodifiableList(plantas); }

    // solo lectura: las asignaciones se cambian desde EmpleadoIndoor
    public List<EmpleadoIndoor> getEmpleadosAsignados() { return Collections.unmodifiableList(empleadosAsignados); }


    public void recibirEvento(Evento e) {
        Planta planta = e.getPlanta();
        if (!plantas.contains(planta)) {
            throw new OperacionInvalidaException("La planta de ese evento no pertenece a este indoor");
        }
        if (planta.isCosechada()) {
            throw new OperacionInvalidaException(
                    "La planta " + planta.getGenetica() + " ya fue cosechada y no recibe eventos");
        }
        colaEventos.add(e);
    }

    /** Si la planta ya tiene un evento sin atender de ese tipo: evita que la revision periodica lo duplique. */
    public boolean tieneEventoPendiente(Planta p, Class<? extends Evento> tipo) {
        for (Evento evento : colaEventos) {
            if (evento.getPlanta() == p && !evento.getRealizado() && tipo.isInstance(evento)) {
                return true;
            }
        }
        return false;
    }

    // lo llama Planta al cosecharse: los pendientes ya no se van a atender, los atendidos quedan como historial
    void descartarEventosPendientesDe(Planta p) {
        colaEventos.removeIf(evento -> evento.getPlanta() == p && !evento.getRealizado());
    }

    /** Todos los eventos del indoor, atendidos o no: los atendidos quedan como historial. */
    public List<Evento> getColaEventos() {
        return new ArrayList<Evento>(colaEventos);
    }

    public List<Evento> getEventosPendientes() {
        List<Evento> pendientes = new ArrayList<>();
        for (Evento evento : colaEventos) {
            if (!evento.getRealizado()) {
                pendientes.add(evento);
            }
        }
        return pendientes;
    }

    public Evento buscarEvento(int eventoId) {
        for (Evento evento : colaEventos) {
            if (evento.getId() == eventoId) {
                return evento;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Indoor{" + nombre + ", plantas=" + plantas.size() + ", eventos=" + colaEventos.size() + '}';
    }
}
