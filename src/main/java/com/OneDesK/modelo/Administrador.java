package com.OneDesK.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Persona que administra el negocio: indoors, empleados, productos, usuarios y reportes.
 * Tiene email y contrasenia como el resto de las personas, para poder ingresar por el login.
 * Sus acciones pasan por AdministradorService, que verifica que quien las pide sea un administrador.
 */
@Entity
@Table(name = "Administrador")
public class Administrador extends Persona {

	Administrador() {
		super();
	}

	public Administrador(String nombre, String apellido, String email, String contrasenia) {
		super(nombre, apellido, email, contrasenia);
	}

	@Override
	public String toString() {
		return "Administrador{" + getNombre() + " " + getApellido() + '}';
	}
}
