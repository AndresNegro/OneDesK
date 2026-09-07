package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.repositories.EmpleadoIndoorRepository;
@Service
public class EmpleadoIndoorServiceImpl implements EmpleadoIndoorService {
	@Autowired
	private EmpleadoIndoorRepository repositorio;
	
	@Override
	public void guardar(EmpleadoIndoor empleadoIndoor) {
		this.repositorio.save(empleadoIndoor);
	}

}
