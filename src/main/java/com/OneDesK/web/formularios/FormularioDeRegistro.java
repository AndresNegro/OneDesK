package com.OneDesK.web.formularios;

/** La solicitud de cuenta que manda un cliente nuevo. */
public class FormularioDeRegistro {

	private String nombre;
	private String apellido;
	private String email;
	private String contrasenia;
	private String repetir;

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getApellido() {
		return apellido;
	}

	public void setApellido(String apellido) {
		this.apellido = apellido;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getContrasenia() {
		return contrasenia;
	}

	public void setContrasenia(String contrasenia) {
		this.contrasenia = contrasenia;
	}

	public String getRepetir() {
		return repetir;
	}

	public void setRepetir(String repetir) {
		this.repetir = repetir;
	}
}
