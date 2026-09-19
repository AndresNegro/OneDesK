package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;
import com.OneDesK.repositories.IndoorRepository;

@Service
public class IndoorServiceImpl implements IndoorService {

	@Autowired
	private IndoorRepository repositorio;

	@Override
	@Transactional
	public Indoor crearIndoor(String nombre, int capacidad) {
		Indoor indoor = new Indoor(nombre, capacidad);
		if (repositorio.existsByNombreIgnoreCase(indoor.getNombre())) {
			throw new OperacionInvalidaException("Ya existe un indoor llamado " + indoor.getNombre());
		}
		return repositorio.save(indoor);
	}

	@Override
	@Transactional
	public Planta plantar(int indoorId, Planta planta) {
		Indoor indoor = buscarIndoor(indoorId);
		if (planta.getIndoor() != null) {
			throw new OperacionInvalidaException("La planta " + planta.getGenetica() + " ya esta plantada en un indoor");
		}
		indoor.addPlanta(planta);

		// flush y no save(indoor): save sobre un indoor existente hace merge, que guarda una copia de la
		// planta y deja sin id a la que se devuelve. Con flush el cascade persiste esta misma planta.
		repositorio.flush();
		return planta;
	}

	@Override
	@Transactional
	public void quitarPlanta(int indoorId, int plantaId) {
		Indoor indoor = buscarIndoor(indoorId);
		Planta planta = indoor.buscarPlanta(plantaId);
		if (planta == null) {
			throw new RecursoNoEncontradoException("El indoor " + indoorId + " no tiene la planta " + plantaId);
		}
		indoor.deletePlanta(planta);
	}

	private Indoor buscarIndoor(int indoorId) {
		return repositorio.findById(indoorId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el indoor " + indoorId));
	}
}
