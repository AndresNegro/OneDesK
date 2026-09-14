package com.OneDesK.services;

import com.OneDesK.modelo.Usuario;

public interface UsuarioService {

	public Usuario registrar(String nombre, String apellido, String email, String contrasenia);

	public Usuario asignarTopeCredito(int usuarioId, int tope);
}
