package com.OneDesK.modelo;
import com.OneDesK.evento.*;

import java.time.LocalDate;

public class Planta extends Thread {
    private String genetica;
    private LocalDate fechaPlantado;
    private LocalDate fechaGerminado;
    private LocalDate fechaCosecha;
    private int tiempoRegado;    // minutos entre riegos
    private int tiempoLuz;       // minutos entre eventos de luz
    private int tiempoVentilacion; // minutos entre eventos de ventilacion
    private boolean luz;
    private boolean ventilador;
    private Indoor indoor;
    private int vecesRegada;

    private final Object candado = new Object();
    private boolean viva = true;
    private boolean comenzar = false;

    // proximos disparos (ms absolutos) por tipo. Long.MAX_VALUE = esperando atencion.
    private long proxRiego;
    private long proxLuz;
    private long proxVent;

    // evento pendiente de atencion por tipo (null = ninguno)
    private EventoRegado pendRiego;
    private EventoLuz pendLuz;
    private EventoVentilador pendVent;

    public Planta(String genetica, LocalDate fechaPlantado, LocalDate fechaGerminado,
                  int tiempoRegado, int tiempoLuz, int tiempoVentilacion) {
        this.genetica = genetica;
        this.fechaPlantado = fechaPlantado;
        this.fechaGerminado = fechaGerminado;
        this.tiempoRegado = tiempoRegado;
        this.tiempoLuz = tiempoLuz;
        this.tiempoVentilacion = tiempoVentilacion;
        long t0 = System.currentTimeMillis();
        proxRiego = t0 + tiempoRegado * 60_000L;
        proxLuz = t0 + tiempoLuz * 60_000L;
        proxVent = t0 + tiempoVentilacion * 60_000L;
    }

    public void setIndoor(Indoor indoor) { this.indoor = indoor; }
    public Indoor getIndoor() { return indoor; }

    @Override
    public void run() {
        // Compuerta de arranque: espera wait() hasta que arrancar() haga notifyAll.
        synchronized (candado) {
            while (!comenzar && !viva) {
                try {
                    candado.wait();
                } catch (InterruptedException e) {
                    if (!viva) return;
                }
            }
            if (!viva) return;
        }

        long t0 = System.currentTimeMillis();
        synchronized (candado) {
            proxRiego = t0 + tiempoRegado * 60_000L;
            proxLuz = t0 + tiempoLuz * 60_000L;
            proxVent = t0 + tiempoVentilacion * 60_000L;
        }

        while (true) {
            long delta;
            synchronized (candado) {
                if (!viva) return;
                long now = System.currentTimeMillis();

                // Reprograma un tipo solo cuando su evento pendiente fue atendido.
                if (pendRiego != null && pendRiego.getRealizado()) {
                    proxRiego = now + tiempoRegado * 60_000L;
                    pendRiego = null;
                }
                if (pendLuz != null && pendLuz.getRealizado()) {
                    proxLuz = now + tiempoLuz * 60_000L;
                    pendLuz = null;
                }
                if (pendVent != null && pendVent.getRealizado()) {
                    proxVent = now + tiempoVentilacion * 60_000L;
                    pendVent = null;
                }

                // Genera el evento del tipo cuyo plazo vencio (y queda esperando atencion).
                if (pendRiego == null && proxRiego <= now) { generarRiego(); proxRiego = Long.MAX_VALUE; }
                if (pendLuz == null && proxLuz <= now) { generarLuz(); proxLuz = Long.MAX_VALUE; }
                if (pendVent == null && proxVent <= now) { generarVentilacion(); proxVent = Long.MAX_VALUE; }

                long min = minProx();
                if (min == Long.MAX_VALUE) {
                    // Todo generado y pendiente de atencion: espera notifyAll del empleado
                    // (notificarAtendido) para reprogramar el tipo atendido.
                    try {
                        candado.wait();
                    } catch (InterruptedException e) {
                        if (!viva) return;
                    }
                    continue;
                }
                delta = Math.max(0, min - now);
            }
            // Temporizador entre disparos (fuera del candado para no bloquear el menu).
            if (delta > 0) {
                try {
                    Thread.sleep(delta);
                } catch (InterruptedException e) {
                    if (!viva) return;
                }
            }
        }
    }

    public String getGenetica() {
    	return this.genetica;
    }
    // Compuerta de arranque: inicia el thread y libera el wait() de run() vía notifyAll().
    public void arrancar() {
        if (getState() == State.NEW) start();
        synchronized (candado) {
            comenzar = true;
            candado.notifyAll();
        }
    }

    public void detener() {
        synchronized (candado) {
            viva = false;
            candado.notifyAll();
        }
        interrupt();
    }

    private long minProx() {
        return Math.min(proxRiego, Math.min(proxLuz, proxVent));
    }

    private void generarRiego() {
        if (indoor != null) {
            pendRiego = new EventoRegado(this);
            indoor.recibirEvento(pendRiego);
        }
    }

    private void generarLuz() {
        if (indoor != null) {
            pendLuz = new EventoLuz(this);
            indoor.recibirEvento(pendLuz);
        }
    }

    private void generarVentilacion() {
        if (indoor != null) {
            pendVent = new EventoVentilador(this);
            indoor.recibirEvento(pendVent);
        }
    }

    // Lo llama el empleado al atender un evento de esta planta: despierta al hilo
    // (notifyAll) para que reprograme el tipo que quedo pendiente.
    public void notificarAtendido() {
        synchronized (candado) {
            candado.notifyAll();
        }
    }

    public void regar() { vecesRegada++; }

    public int proximoRiegoMin() { return restanteMin(proxRiego); }
    public int proximoLuzMin() { return restanteMin(proxLuz); }
    public int proximoVentilacionMin() { return restanteMin(proxVent); }

    private int restanteMin(long deadline) {
        if (deadline == Long.MAX_VALUE) return -1; // esperando atencion
        long r = (deadline - System.currentTimeMillis()) / 60_000L;
        return r < 0 ? 0 : (int) r;
    }

	public boolean isLuz() {
		return this.luz;
	}

	public void setLuz(boolean b) {
		this.luz=b;
		
	}

	public boolean isVentilador() {
		return this.ventilador;
	}

	public void setVentilador(boolean b) {
		this.ventilador=b;
	}
		
	}