package com.OneDesK.modelo;

public class RegistroProduccion {
    private Indoor indoor;
    private EmpleadoIndoor empleado;
    private String genetica;
    private int cantidad;

    public RegistroProduccion(Indoor indoor, EmpleadoIndoor empleado, String genetica, int cantidad) {
        this.indoor = indoor;
        this.empleado = empleado;
        this.genetica = genetica;
        this.cantidad = cantidad;
    }

    public void setIndoor(Indoor i) { this.indoor = i; }
    public Indoor getIndoor() { return indoor; }
    public void setEmpleado(EmpleadoIndoor ei) { this.empleado = ei; }
    public EmpleadoIndoor getEmpleadoIndoor() { return empleado; }
    public void setGenetica(String g) { this.genetica = g; }
    public String getGenetica() { return genetica; }
    public void setCantidad(int cant) { this.cantidad = cant; }
    public int getCantidad() { return cantidad; }

    @Override
    public String toString() {
        return "RegistroProduccion{" + genetica + " x" + cantidad +
                ", indoor=" + indoor + ", empleado=" + (empleado == null ? "null" : empleado.getNombre()) + '}';
    }
}
