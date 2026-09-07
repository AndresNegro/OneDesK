package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.OneDesK.modelo.RegistroProduccion;
import com.OneDesK.repositories.RegistroProduccionRepository;


@Service
public class RegistroProduccionServiceImpl implements RegistroProduccionService {

	@Autowired
	private RegistroProduccionRepository repositorio;
	
	@Override
	public void guardar(RegistroProduccion registroProduccion) {
		this.repositorio.save(registroProduccion);
	}

}
