package com.OneDesK.modelo;
import com.OneDesK.evento.*;
import com.OneDesK.excepciones.OperacionInvalidaException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name="Planta")
public class Planta extends Persistible{
	@Column(name="genetica")
    private String genetica;
	@Column(name="fechaPlantado")
    private LocalDate fechaPlantado;
	@Column(name="fechaGerminado")
    private LocalDate fechaGerminado;
	@Column(name="fechaCosecha")
    private LocalDate fechaCosecha;
	@Column(name="tiempoRegado")
    private int tiempoRegado;    // minutos entre riegos
	@Column(name="tiempoLuz")
    private int tiempoLuz;       // minutos entre eventos de luz
	@Column(name="tiempoVentilacion")
    private int tiempoVentilacion; // minutos entre eventos de ventilacion
	@Column(name="luz")
    private boolean luz;
	@Column(name="ventilador")
    private boolean ventilador;
	@ManyToOne
	@JoinColumn(name="ID_INDOOR")
    private Indoor indoor;
    @Column(name="ultimoRegado")
    private LocalDate ultimoRegado;
    @Column(name="ultimoLuz")
    private LocalDate ultimoLuz;
    @Column(name="ultimoVentilacion")
    private LocalDate ultimoVentilacion;
    
    Planta(){
    	
    }
    
    public Planta(String genetica, LocalDate fechaPlantado, LocalDate fechaGerminado,
                  int tiempoRegado, int tiempoLuz, int tiempoVentilacion) {
        if (genetica == null || genetica.isBlank()) {
            throw new IllegalArgumentException("La genetica no puede estar vacia");
        }
        if (fechaPlantado == null || fechaGerminado == null) {
            throw new IllegalArgumentException("Las fechas de germinado y plantado son obligatorias");
        }
        if (fechaPlantado.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de plantado no puede ser futura");
        }
        // la semilla germina antes de plantarse: como el plantado no es futuro, el germinado tampoco
        if (fechaGerminado.isAfter(fechaPlantado)) {
            throw new IllegalArgumentException("La planta no puede germinar despues de plantada");
        }
        if (tiempoRegado <= 0 || tiempoLuz <= 0 || tiempoVentilacion <= 0) {
            throw new IllegalArgumentException("Los tiempos de riego, luz y ventilacion tienen que ser positivos");
        }
        this.genetica = genetica.trim();
        this.fechaPlantado = fechaPlantado;
        this.fechaGerminado = fechaGerminado;
        this.tiempoRegado = tiempoRegado;
        this.tiempoLuz = tiempoLuz;
        this.tiempoVentilacion = tiempoVentilacion;
    }

    public void setIndoor(Indoor indoor) { this.indoor = indoor; }
    public Indoor getIndoor() { return indoor; }

    public LocalDate getFechaCosecha() { return fechaCosecha; }

    public boolean isCosechada() { return fechaCosecha != null; }

    public void cosechar() {
        if (isCosechada()) {
            throw new OperacionInvalidaException("La planta " + genetica + " ya fue cosechada el " + fechaCosecha);
        }
        this.fechaCosecha = LocalDate.now();
    }

   
    public String getGenetica() {
    	return this.genetica;
    }

    private void generarRiego() {
        if (indoor != null) {
            indoor.recibirEvento(new EventoRegado(this));
        }
    }

    private void generarLuz() {
        if (indoor != null) {
            indoor.recibirEvento(new EventoLuz(this));
        }
    }

    private void generarVentilacion() {
        if (indoor != null) {
            indoor.recibirEvento(new EventoVentilador(this));
        }
    }

    public void regar() { ultimoRegado= LocalDate.now();}


	public boolean isLuz() {
		return this.luz;
	}

	public void setLuz(boolean b) {
		this.luz=b;
		 ultimoLuz= LocalDate.now();
		
	}

	public boolean isVentilador() {
		return this.ventilador;
	}

	public void setVentilador(boolean b) {
		this.ventilador=b;
		 ultimoVentilacion= LocalDate.now();
	}
		
	}