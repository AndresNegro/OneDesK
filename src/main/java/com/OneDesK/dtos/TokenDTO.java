package com.OneDesK.dtos;

/** Lo que devuelve el login de la API: el token con el que se piden las demas direcciones. */
public class TokenDTO implements DTO {

	private String token;
	private String tipo;
	private long minutos;

	public TokenDTO() {
	}

	public TokenDTO(String token, String tipo, long minutos) {
		this.token = token;
		this.tipo = tipo;
		this.minutos = minutos;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	/** Como se manda despues: "Bearer", el esquema que espera la cabecera Authorization. */
	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public long getMinutos() {
		return minutos;
	}

	public void setMinutos(long minutos) {
		this.minutos = minutos;
	}
}
