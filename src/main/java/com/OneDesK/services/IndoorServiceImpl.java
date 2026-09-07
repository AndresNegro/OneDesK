package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.OneDesK.modelo.Indoor;
import com.OneDesK.repositories.IndoorRepository;

@Service
public class IndoorServiceImpl implements IndoorService {

	@Autowired
	private IndoorRepository repositorio;
	
	
	@Override
	public void guardar(Indoor indoor) {
		this.repositorio.save(indoor);
	}

}
