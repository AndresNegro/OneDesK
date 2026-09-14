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
    @Column(name="email", unique=true)
    private String email;
    @Column(name="contraseña")
    private String contrasenia;

    Persona(){

    }

    protected Persona(String nombre, String apellido, String email, String contrasenia) {
        setNombre(nombre);
        setApellido(apellido);
        setEmail(email);
        setContrasenia(contrasenia);
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) {
        if (!ValidationUtils.tieneTexto(nombre)) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
        this.nombre = nombre.trim();
    }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) {
        if (!ValidationUtils.tieneTexto(apellido)) {
            throw new IllegalArgumentException("El apellido no puede estar vacio");
        }
        this.apellido = apellido.trim();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        if (!ValidationUtils.isValidEmail(email)) {
            throw new IllegalArgumentException("El email no es valido: " + email);
        }
        this.email = email.trim().toLowerCase();
    }

    public String getContrasenia() { return contrasenia; }
    public void setContrasenia(String contrasenia) {
        if (!ValidationUtils.tieneMasDe(contrasenia, 5)) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 5 caracteres");
        }
        this.contrasenia = contrasenia;
    }

    @Override
    public String toString() {
        return getNombre() + " " + getApellido() + " <" + getEmail() + ">";
    }
}
