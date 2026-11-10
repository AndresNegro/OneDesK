package com.OneDesK.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.OneDesK.enums.Permiso;
import com.OneDesK.enums.Rol;
import com.OneDesK.excepciones.DatoInvalidoException;

import com.OneDesK.helpers.ValidationUtils;

import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name="Persona")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Persona extends Persistible {
    // con que empiezan los hashes de BCrypt ($2a, $2b, $2y): sirve para no encriptar dos veces
    private static final String MARCA_DE_BCRYPT = "$2";

    @Column(name="nombre")
	private String nombre;
    @Column(name="apellido")
    private String apellido;
    @Column(name="email", unique=true)
    private String email;
    @Column(name="contraseña")
    private String contrasenia;
    // se leen al ingresar, cuando ya no hay transaccion abierta para ir a buscarlos
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "Persona_Rol", joinColumns = @JoinColumn(name = "ID_PERSONA"))
    @Column(name = "ID_ROL")
    @Convert(converter = RolComoId.class)
    private List<Rol> roles = new ArrayList<>();

    Persona(){

    }

    protected Persona(String nombre, String apellido, String email, String contrasenia) {
        setNombre(nombre);
        setApellido(apellido);
        setEmail(email);
        setContrasenia(contrasenia);
    }

    /**
     * El rol se lo pone cada subclase al construirse: no es un dato que se edite, sale de que esta
     * persona es un cliente, un empleado o el administrador.
     */
    protected void tomarRol(Rol rol) {
        if (!roles.contains(rol)) {
            roles.add(rol);
        }
    }

    public List<Rol> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    /** Con que rol entra a la aplicacion: en OneDesK cada persona es una sola cosa. */
    @JsonIgnore
    public Rol getRol() {
        return roles.get(0);
    }

    /**
     * Si puede entrar a la aplicacion. Las personas del negocio entran siempre; el cliente recien
     * cuando la administracion le aprueba la cuenta, y por eso Usuario lo responde distinto.
     */
    public boolean estaHabilitada() {
        return true;
    }

    /** Si puede hacer esa accion. Lo contesta la persona: afuera no se recorren sus roles. */
    public boolean puede(Permiso permiso) {
        return roles.stream().anyMatch(rol -> rol.puede(permiso));
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) {
        if (!ValidationUtils.tieneTexto(nombre)) {
            throw new DatoInvalidoException("error.nombre.vacio");
        }
        this.nombre = nombre.trim();
    }

    /** Nombre y apellido juntos: se llama igual que en el DTO para que el mapeo sea directo. */
    public String getNombreCompleto() {
        return getNombre() + " " + getApellido();
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

    /**
     * Guarda encriptada la contrasenia que se recibio en claro. La llaman los services apenas crean
     * la persona: en la base no queda ninguna contrasenia legible. Si ya estaba encriptada no hace
     * nada, asi volver a llamarla (o la migracion de las viejas) no encripta el hash otra vez.
     */
    public void encriptarContrasenia(PasswordEncoder encriptador) {
        if (!tieneLaContraseniaEncriptada()) {
            this.contrasenia = encriptador.encode(this.contrasenia);
        }
    }

    public boolean tieneLaContraseniaEncriptada() {
        return contrasenia != null && contrasenia.startsWith(MARCA_DE_BCRYPT);
    }

    // la API devuelve personas: la contrasenia no viaja nunca, ni siquiera cifrada
    @JsonIgnore
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
