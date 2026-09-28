package com.OneDesK.modelo;

import com.OneDesK.excepciones.DatoInvalidoException;

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
            throw new DatoInvalidoException("error.nombre.vacio");
        }
        this.nombre = nombre.trim();
    }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) {
        if (!ValidationUtils.tieneTexto(apellido)) {
            throw new DatoInvalidoException("error.apellido.vacio");
        }
        this.apellido = apellido.trim();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        if (!ValidationUtils.isValidEmail(email)) {
            throw new DatoInvalidoException("error.email.invalido", email);
        }
        this.email = email.trim().toLowerCase();
    }

    public String getContrasenia() { return contrasenia; }
    public void setContrasenia(String contrasenia) {
        if (!ValidationUtils.tieneMasDe(contrasenia, 5)) {
            throw new DatoInvalidoException("error.contrasenia.corta");
        }
        this.contrasenia = contrasenia;
    }

    @Override
    public String toString() {
        return getNombre() + " " + getApellido() + " <" + getEmail() + ">";
    }
}
