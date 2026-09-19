package com.OneDesK.services;

import com.OneDesK.modelo.Usuario;

public interface UsuarioService {

	/** Guarda la solicitud de registro: el usuario queda pendiente hasta que lo apruebe el administrador. */
	public Usuario registrar(String nombre, String apellido, String email, String contrasenia);

	public Usuario asignarTopeCredito(int usuarioId, int tope);

	public Usuario buscar(int usuarioId);
}
