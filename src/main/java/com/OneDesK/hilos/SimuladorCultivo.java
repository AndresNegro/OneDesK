package com.OneDesK.hilos;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;

import java.util.List;

public class SimuladorCultivo {
    private final List<Indoor> indoors;

    public SimuladorCultivo(List<Indoor> indoors) {
        this.indoors = indoors;
    }

    // Arranca todas las plantas de todos los indoors (cada una con su compuerta
    // de arranque vía wait/notifyAll).
    public void arrancar() {
        for (Indoor in : indoors) {
            for (Planta p : in.getPlantas()) {
                p.arrancar();
            }
        }
    }

    public void arrancarPlanta(Planta p) {
        p.arrancar();
    }

    public void detener() {
        for (Indoor in : indoors) {
            for (Planta p : in.getPlantas()) {
                p.detener();
            }
        }
    }

    public boolean hayEventosPendientes() {
        for (Indoor in : indoors) {
            if (in.colaSize() > 0) return true;
        }
        return false;
    }
}
