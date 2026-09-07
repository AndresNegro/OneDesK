package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.OneDesK.modelo.Compra;
import com.OneDesK.repositories.CompraRepository;

@Service
public class CompraServiceImpl implements CompraService {
	@Autowired
	private CompraRepository repositorio;
	
	@Override
	public void guardar(Compra compra) {
		this.repositorio.save(compra);

	}

}
