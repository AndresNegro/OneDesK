package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.OneDesK.modelo.ItemCompra;
import com.OneDesK.repositories.ItemCompraRepository;

@Service
public class ItemCompraServiceImpl implements ItemCompraService {

	@Autowired
	private ItemCompraRepository repositorio;
	
	@Override
	public void guardar(ItemCompra itemCompra) {
		this.repositorio.save(itemCompra);
	}

}
