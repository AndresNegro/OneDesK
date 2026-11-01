package com.OneDesK.modelo;

import com.OneDesK.dtos.DTO;

/**
 * Lo implementa la entidad que sale por la API. Ya no arma el DTO: solo dice con cual sale, y el
 * mapeador lo construye copiando los campos que se llaman igual (ver ConfiguracionDeMapeo).
 *
 * Son dos porque no todos ven lo mismo: la administracion necesita datos que al resto no se le
 * muestran. Cuando no hay diferencia, las dos devuelven el mismo DTO.
 */
public interface Exponible {

	/** El DTO con el que esta entidad sale por la API publica. */
	Class<? extends DTO> dtoParaApi();

	/** El DTO con el que sale en los listados de la administracion. */
	Class<? extends DTO> dtoParaAdmin();
}
