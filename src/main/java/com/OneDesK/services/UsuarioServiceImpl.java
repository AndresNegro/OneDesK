package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.OneDesK.modelo.Usuario;
import com.OneDesK.repositories.UsuarioRepository;

@Service
public class UsuarioServiceImpl implements UsuarioService {
	@Autowired
	private UsuarioRepository repositorio;

	@Override
	public void guardar(Usuario user) {
	this.repositorio.save(user);
	}
}
