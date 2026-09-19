package com.OneDesK.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.evento.Evento;
import com.OneDesK.excepciones.EmailDuplicadoException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.excepciones.OperacionInvalidaException;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;
import com.OneDesK.repositories.EmpleadoIndoorRepository;
import com.OneDesK.repositories.IndoorRepository;
import com.OneDesK.repositories.PersonaRepository;

@Service
public class EmpleadoIndoorServiceImpl implements EmpleadoIndoorService {

	@Autowired
	private EmpleadoIndoorRepository repositorio;
	@Autowired
	private IndoorRepository indoorRepository;
	@Autowired
	private PersonaRepository personaRepository;
	@Autowired
	private IndoorService indoorService;

	@Override
	@Transactional
	public EmpleadoIndoor registrar(String nombre, String apellido, String email, String contrasenia,
			int salarioMensual) {
		EmpleadoIndoor empleado = new EmpleadoIndoor(nombre, apellido, email, contrasenia, salarioMensual);

		if (personaRepository.existsByEmail(empleado.getEmail())) {
			throw new EmailDuplicadoException("Ya existe una persona registrada con el email " + empleado.getEmail());
		}
		return repositorio.save(empleado);
	}

	@Override
	@Transactional
	public EmpleadoIndoor buscar(int empleadoId) {
		return buscarEmpleado(empleadoId);
	}

	@Override
	@Transactional
	public Planta plantar(int empleadoId, int indoorId, Planta planta) {
		EmpleadoIndoor empleado = buscarEmpleado(empleadoId);
		if (!empleado.estaAsignadoA(buscarIndoor(indoorId))) {
			throw new OperacionInvalidaException("No estas asignado al indoor " + indoorId);
		}
		return indoorService.plantar(indoorId, planta);
	}

	@Override
	@Transactional
	public EmpleadoIndoor cambiarSalario(int empleadoId, int salarioMensual) {
		EmpleadoIndoor empleado = buscarEmpleado(empleadoId);
		empleado.setSalarioMensual(salarioMensual);
		return empleado;
	}

	@Override
	@Transactional
	public void asignarIndoor(int empleadoId, int indoorId) {
		buscarEmpleado(empleadoId).addIndoor(buscarIndoor(indoorId));
	}

	@Override
	@Transactional
	public void desasignarIndoor(int empleadoId, int indoorId) {
		buscarEmpleado(empleadoId).deleteIndoor(buscarIndoor(indoorId));
	}

	@Override
	@Transactional
	public List<Evento> eventosPendientes(int empleadoId) {
		return buscarEmpleado(empleadoId).eventosPendientes();
	}

	@Override
	@Transactional
	public void atenderEvento(int empleadoId, int indoorId, int eventoId) {
		EmpleadoIndoor empleado = buscarEmpleado(empleadoId);
		Indoor indoor = buscarIndoor(indoorId);

		Evento evento = indoor.buscarEvento(eventoId);
		if (evento == null) {
			throw new RecursoNoEncontradoException("El indoor " + indoorId + " no tiene el evento " + eventoId);
		}
		empleado.atenderEvento(evento);
	}

	private EmpleadoIndoor buscarEmpleado(int empleadoId) {
		return repositorio.findById(empleadoId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el empleado " + empleadoId));
	}

	private Indoor buscarIndoor(int indoorId) {
		return indoorRepository.findById(indoorId)
				.orElseThrow(() -> new RecursoNoEncontradoException("No existe el indoor " + indoorId));
	}
}
