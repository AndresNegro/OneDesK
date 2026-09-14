package com.OneDesK.modelo;

import com.OneDesK.helpers.ValidationUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Table(name="Persona")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Persona extends Persistible {
    @Column(name="nombre")
	private String nombre;
    @Column(name="apellido")
    private String apellido;
    @Column(name="email")
    private String email;
    @Column(name="contraseña")
    private String contrasenia;
    
    Persona(){
    	
    }

    protected Persona(String nombre, String apellido, String email, String contrasenia) {
        this.nombre = nombre;
        this.apellido = apellido;
        if(!ValidationUtils.isValidEmail(email)) {
        	throw new IllegalArgumentException("el Email no es valido");
        }
        this.email = email;
        if(!ValidationUtils.tieneMasDe(contrasenia, 5)) {
        	throw new IllegalArgumentException("La contraseña es menor a 5 caracteres");
        }
        this.contrasenia = contrasenia;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContrasenia() { return contrasenia; }
    public void setContrasenia(String contrasenia) { this.contrasenia = contrasenia; }

    @Override
    public String toString() {
        return getNombre() + " " + getApellido() + " <" + getEmail() + ">";
    }
}