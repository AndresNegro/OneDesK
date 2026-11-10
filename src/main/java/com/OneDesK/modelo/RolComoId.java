package com.OneDesK.modelo;

import com.OneDesK.enums.Rol;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** El rol se guarda por el id que declara, el mismo de la tabla Rol, y no por su posicion en el enum. */
@Converter
public class RolComoId implements AttributeConverter<Rol, Integer> {

	@Override
	public Integer convertToDatabaseColumn(Rol rol) {
		return rol == null ? null : rol.getId();
	}

	@Override
	public Rol convertToEntityAttribute(Integer id) {
		return id == null ? null : Rol.delId(id);
	}
}
