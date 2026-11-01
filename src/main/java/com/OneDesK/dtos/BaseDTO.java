package com.OneDesK.dtos;

/**
 * Lo que tienen todos los DTO: el id con el que quien consume la API vuelve a pedir ese recurso.
 * Los demas heredan de aca y agregan lo suyo.
 */
public abstract class BaseDTO implements DTO {

	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
