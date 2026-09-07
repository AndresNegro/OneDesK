package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.OneDesK.modelo.Deuda;
import com.OneDesK.repositories.DeudaRepository;

@Service
public class DeudaServiceImpl implements DeudaService {
	@Autowired
	private DeudaRepository repositorio;
	
	@Override
	public void guardar(Deuda deuda) {
		this.repositorio.save(deuda);
	}

}
