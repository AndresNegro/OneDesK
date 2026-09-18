package com.OneDesK.modelo;

import com.OneDesK.evento.Evento;
import com.OneDesK.excepciones.OperacionInvalidaException;

import jakarta.persistence.CascadeType;
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
	// lado inverso: las asignaciones se hacen desde EmpleadoIndoor, que es quien escribe en Trabaja
	@ManyToMany(mappedBy = "sectoresACargo")
	private List<EmpleadoIndoor> empleadosAsignados;
	@OneToMany(mappedBy = "indoor", cascade= CascadeType.ALL, orphanRemoval = true )
    private List<Planta> plantas;
	@OneToMany(cascade= CascadeType.ALL, orphanRemoval = true )
	@JoinColumn(name = "ID_INDOOR", referencedColumnName="ID", nullable = false)
    private List<Evento> colaEventos;

    public Indoor() {
    	this.empleadosAsignados = new ArrayList<>();
        this.plantas = new ArrayList<>();
        this.colaEventos = new ArrayList<>();
    }

    public Planta addPlanta(Planta p) {
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
        return "Indoor{plantas=" + plantas.size() + ", eventos=" + colaEventos.size() + '}';
    }
}
