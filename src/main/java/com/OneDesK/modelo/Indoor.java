package com.OneDesK.modelo;

import com.OneDesK.evento.Evento;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name="Indoor")
public class Indoor extends Persistible{
	@ManyToMany(mappedBy = "sectoresACargo")
	private List<EmpleadoIndoor> empleadosAsignados;
	@OneToMany(mappedBy = "indoor", cascade= CascadeType.ALL, orphanRemoval = true )
    private List<Planta> plantas;
	@OneToMany(cascade= CascadeType.ALL, orphanRemoval = true )
	@JoinColumn(name = "ID_INDOOR", referencedColumnName="ID")
    private List<Evento> colaEventos;
	
    public Indoor() {
    	this.empleadosAsignados = new ArrayList<>();
        this.plantas = new ArrayList<>();
        this.colaEventos = new ArrayList<>();
    }
    
    public void addEmpleado(EmpleadoIndoor E) {
    	this.empleadosAsignados.add(E);
    }
    
    public void deleteEmpleado(EmpleadoIndoor E) {
    	this.empleadosAsignados.remove(E);
    }

    public Planta addPlanta(Planta p) {
        plantas.add(p);
        p.setIndoor(this);
        return p;
    }

    public void deletePlanta(Planta p) {
        plantas.remove(p);
        if (p.getIndoor() == this) p.setIndoor(null);
    }

    public List<Planta> getPlantas() { return Collections.unmodifiableList(plantas); }

    
    public void recibirEvento(Evento e) {
        colaEventos.add(e);
    }

    public List<Evento> getColaEventos() {
        return new ArrayList<Evento>(colaEventos);
    }

    public Evento consumirEvento(int index) {
        if (index < 0 || index >= colaEventos.size()) return null;
        return colaEventos.remove(index);
    }

    public int colaSize() { return colaEventos.size(); }

    @Override
    public String toString() {
        return "Indoor{plantas=" + plantas.size() + ", cola=" + colaEventos.size() + '}';
    }
}
