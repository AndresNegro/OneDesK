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
			throw new OperacionInvalidaException("error.indoor.repetido", indoor.getNombre());
		}
		return repositorio.save(indoor);
	}

	@Override
	@Transactional
	public Indoor editarIndoor(int indoorId, String nombre, int capacidad) {
		Indoor indoor = buscarIndoor(indoorId);
		// el nombre se compara ya sin espacios de mas, como lo guarda Indoor
		String nombreNuevo = nombre == null ? null : nombre.trim();
		if (nombreNuevo != null && repositorio.existsByNombreIgnoreCaseAndIdNot(nombreNuevo, indoorId)) {
			throw new OperacionInvalidaException("error.indoor.repetido", nombreNuevo);
		}
		// sin save: el indoor ya esta guardado y el cambio se escribe al terminar la transaccion
		indoor.editar(nombreNuevo, capacidad);
		return indoor;
	}

	@Override
	@Transactional
	public Planta plantar(int indoorId, Planta planta) {
		Indoor indoor = buscarIndoor(indoorId);
		if (planta.getIndoor() != null) {
			throw new OperacionInvalidaException("error.planta.ya.plantada", planta.getGenetica());
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
			throw new RecursoNoEncontradoException("error.indoor.sin.planta", indoorId, plantaId);
		}
		indoor.deletePlanta(planta);
	}

	private Indoor buscarIndoor(int indoorId) {
		return repositorio.findById(indoorId)
				.orElseThrow(() -> new RecursoNoEncontradoException("error.no.existe.indoor", indoorId));
	}
}
