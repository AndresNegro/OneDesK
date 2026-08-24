package com.OneDesK.modelo;

public abstract class Empleado extends Persona {
    protected Empleado(String nombre, String apellido, String email, String contrasenia) {
        super(nombre, apellido, email, contrasenia);
    }
}
