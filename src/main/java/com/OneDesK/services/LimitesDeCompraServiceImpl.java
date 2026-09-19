package com.OneDesK.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.modelo.LimitesDeCompra;
import com.OneDesK.repositories.LimitesDeCompraRepository;

@Service
public class LimitesDeCompraServiceImpl implements LimitesDeCompraService {

	@Autowired
	private LimitesDeCompraRepository repositorio;

	// hay una sola fila: la primera vez que se pide, se guarda con los valores iniciales
	@Override
	@Transactional
	public LimitesDeCompra obtener() {
		List<LimitesDeCompra> guardados = repositorio.findAll();
		if (!guardados.isEmpty()) {
			return guardados.get(0);
		}
		return repositorio.save(new LimitesDeCompra());
	}

	@Override
	@Transactional
	public LimitesDeCompra cambiar(int minimoGramos, int maximoGramos) {
		LimitesDeCompra limites = obtener();
		limites.cambiar(minimoGramos, maximoGramos);
		return limites;
	}
}
