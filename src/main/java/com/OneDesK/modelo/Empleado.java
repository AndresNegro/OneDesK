package com.OneDesK.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name ="Empleado")
public abstract class Empleado extends Persona {
	
	Empleado(){
		super();
	}
	
    protected Empleado(String nombre, String apellido, String email, String contrasenia) {
        super(nombre, apellido, email, contrasenia);
    }
}
