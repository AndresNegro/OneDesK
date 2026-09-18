package com.OneDesK.services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;
import com.OneDesK.repositories.IndoorRepository;

@Service
public class GeneradorDeEventosServiceImpl implements GeneradorDeEventosService {

	@Autowired
	private IndoorRepository repositorio;

	// sin save: los eventos nuevos entran a la cola de un indoor ya guardado y el cascade
	// los inserta al terminar la transaccion
	@Override
	@Transactional
	public int generarEventos() {
		LocalDateTime ahora = LocalDateTime.now();
		int creados = 0;
		for (Indoor indoor : repositorio.findAll()) {
			for (Planta planta : indoor.getPlantas()) {
				creados += planta.generarEventosVencidos(ahora);
			}
		}
		return creados;
	}
}
