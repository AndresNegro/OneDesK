package com.OneDesK.services;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.dtos.DTO;
import com.OneDesK.modelo.Exponible;

/**
 * Entrega en DTOs lo que cada service decida mostrar. La conversion esta escrita una sola vez: el
 * service que hereda solo dice que entidades expone, y cada entidad dice con que DTO sale.
 *
 * Va en el service y no en la API porque la conversion necesita la transaccion abierta: armar el
 * DTO de un cliente, por ejemplo, le pregunta la deuda, que se recalcula leyendo sus compras.
 */
public abstract class ExponibleServiceImpl<E extends Exponible> implements ExponibleService {

	@Autowired
	private ModelMapper mapeador;

	@Override
	@Transactional
	public List<DTO> exponer() {
		List<E> entidades = aExponer();

		return entidades.stream().map(entidad -> mapeador.map(entidad, entidad.dtoParaApi()))
				.collect(Collectors.toList());
	}

	/** Las entidades que este service muestra por la API. */
	protected abstract List<E> aExponer();
}
