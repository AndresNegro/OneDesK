package com.OneDesK.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Table(name ="Empleado")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Empleado extends Persona {
    protected Empleado(String nombre, String apellido, String email, String contrasenia) {
        super(nombre, apellido, email, contrasenia);
    }
}
