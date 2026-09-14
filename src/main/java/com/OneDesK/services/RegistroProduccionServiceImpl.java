package com.OneDesK.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.excepciones.ProductoNoEncontradoException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;
import com.OneDesK.modelo.Producto;
import com.OneDesK.modelo.RegistroProduccion;
import com.OneDesK.repositories.EmpleadoIndoorRepository;
import com.OneDesK.repositories.IndoorRepository;
import com.OneDesK.repositories.ProductoRepository;
import com.OneDesK.repositories.RegistroProduccionRepository;

@Service
public class RegistroProduccionServiceImpl implements RegistroProduccionService {

	@Autowired
	private RegistroProduccionRepository repositorio;
	@Autowired
	private EmpleadoIndoorRepository empleadoRepository;
	@Autowired
	private IndoorRepository indoorRepository;
	@Autowired
	private ProductoRepository productoRepository;

	@Override
	@Transactional
	public RegistroProduccion registrarCosecha(int empleadoId, int indoorId, int plantaId, int cantidad) {
		EmpleadoIndoor empleado = empleadoRepository.findById(empleadoId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el empleado " + empleadoId));
		Indoor indoor = indoorRepository.findById(indoorId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el indoor " + indoorId));
		Planta planta = buscarPlanta(indoor, plantaId);

		if (!empleado.estaAsignadoA(indoor)) {
			throw new OperacionInvalidaException(
					"El empleado " + empleadoId + " no esta asignado al indoor " + indoorId);
		}

		Producto producto = productoRepository.findByGeneticaIgnoreCase(planta.getGenetica())
				.orElseThrow(() -> new ProductoNoEncontradoException(planta.getGenetica()));

		// el registro se arma antes de modificar nada: si la cantidad es invalida, planta y stock quedan intactos
		RegistroProduccion registro = new RegistroProduccion(planta, empleado, producto, cantidad);
		planta.cosechar();
		producto.reponerStock(cantidad);

		return repositorio.save(registro);
	}

	private Planta buscarPlanta(Indoor indoor, int plantaId) {
		for (Planta planta : indoor.getPlantas()) {
			if (planta.getId() == plantaId) {
				return planta;
			}
		}
		throw new RecursoNoEncontradoException("El indoor " + indoor.getId() + " no tiene la planta " + plantaId);
	}
}
