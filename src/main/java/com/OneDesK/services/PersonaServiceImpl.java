package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.OneDesK.modelo.Persona;
import com.OneDesK.repositories.PersonaRepository;


@Service
public class PersonaServiceImpl implements PersonaService {

	@Autowired
	private PersonaRepository repositorio;
	
	@Override
	public void guardar(Persona persona) {
		this.repositorio.save(persona);
	}

}
