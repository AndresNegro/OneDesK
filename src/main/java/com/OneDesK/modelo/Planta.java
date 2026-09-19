package com.OneDesK.modelo;
import com.OneDesK.evento.*;
import com.OneDesK.excepciones.OperacionInvalidaException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    // momento de la ultima atencion de cada tipo: el tiempo hasta el proximo evento corre desde aca
    @Column(name="ultimoRegado")
    private LocalDateTime ultimoRegado;
    @Column(name="ultimoLuz")
    private LocalDateTime ultimoLuz;
    @Column(name="ultimoVentilacion")
    private LocalDateTime ultimoVentilacion;
    
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

    // sin public: solo Indoor la cambia, desde addPlanta y deletePlanta
    void setIndoor(Indoor indoor) { this.indoor = indoor; }
    public Indoor getIndoor() { return indoor; }

    public LocalDate getFechaPlantado() { return fechaPlantado; }
    public LocalDate getFechaGerminado() { return fechaGerminado; }
    public LocalDate getFechaCosecha() { return fechaCosecha; }
    public int getTiempoRegado() { return tiempoRegado; }
    public int getTiempoLuz() { return tiempoLuz; }
    public int getTiempoVentilacion() { return tiempoVentilacion; }

    public boolean isCosechada() { return fechaCosecha != null; }

    public void cosechar() {
        if (isCosechada()) {
            throw new OperacionInvalidaException("La planta " + genetica + " ya fue cosechada el " + fechaCosecha);
        }
        this.fechaCosecha = LocalDate.now();
        if (indoor != null) {
            indoor.descartarEventosPendientesDe(this);
        }
    }

   
    public String getGenetica() {
    	return this.genetica;
    }

    // lo llama Indoor al agregarla: los tres tiempos empiezan a correr desde que la planta entra al indoor
    void empezarACorrerTiempos(LocalDateTime momento) {
        this.ultimoRegado = momento;
        this.ultimoLuz = momento;
        this.ultimoVentilacion = momento;
    }

    /**
     * Crea en su indoor los eventos cuyo tiempo ya se cumplio y devuelve cuantos creo.
     * Un tipo de evento no se repite mientras haya uno sin atender: su tiempo recien vuelve
     * a correr cuando el empleado lo atiende.
     */
    public int generarEventosVencidos(LocalDateTime ahora) {
        if (indoor == null || isCosechada()) {
            return 0;
        }
        int creados = 0;
        if (seCumplioElTiempo(ultimoRegado, tiempoRegado, ahora)
                && !indoor.tieneEventoPendiente(this, EventoRegado.class)) {
            indoor.recibirEvento(new EventoRegado(this));
            creados++;
        }
        if (seCumplioElTiempo(ultimoLuz, tiempoLuz, ahora)
                && !indoor.tieneEventoPendiente(this, EventoLuz.class)) {
            indoor.recibirEvento(new EventoLuz(this));
            creados++;
        }
        if (seCumplioElTiempo(ultimoVentilacion, tiempoVentilacion, ahora)
                && !indoor.tieneEventoPendiente(this, EventoVentilador.class)) {
            indoor.recibirEvento(new EventoVentilador(this));
            creados++;
        }
        return creados;
    }

    // se cumplio cuando ultimo + minutos ya no es posterior a ahora (o sea, es anterior o igual)
    private boolean seCumplioElTiempo(LocalDateTime ultimo, int minutos, LocalDateTime ahora) {
        return ultimo != null && !ultimo.plusMinutes(minutos).isAfter(ahora);
    }

    public void regar() { ultimoRegado = LocalDateTime.now(); }

    public LocalDateTime getUltimoRegado() { return ultimoRegado; }
    public LocalDateTime getUltimoLuz() { return ultimoLuz; }
    public LocalDateTime getUltimoVentilacion() { return ultimoVentilacion; }

    public boolean isLuz() {
        return this.luz;
    }

    public void setLuz(boolean b) {
        this.luz = b;
        ultimoLuz = LocalDateTime.now();
    }

    public boolean isVentilador() {
        return this.ventilador;
    }

    public void setVentilador(boolean b) {
        this.ventilador = b;
        ultimoVentilacion = LocalDateTime.now();
    }
}
