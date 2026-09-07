package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.OneDesK.modelo.Planta;
import com.OneDesK.repositories.PlantaRepository;

@Service
public class PlantaServiceImpl implements PlantaService {

	@Autowired
	private PlantaRepository repositorio;
	
	@Override
	public void guardar(Planta planta) {
		this.repositorio.save(planta);
	}

}
