package com.OneDesK.web;

import com.OneDesK.enums.Rol;

/** A que pagina entra cada rol al ingresar. Vive en la web porque es una direccion, no un dato del negocio. */
public enum PaginaDeInicio {

	DEL_CLIENTE(Rol.USUARIO, CatalogoController.CATALOGO_URL),
	DEL_EMPLEADO(Rol.EMPLEADO, EmpleadoController.EMPLEADO_URL),
	DE_LA_ADMINISTRACION(Rol.ADMINISTRADOR, AdminController.ADMIN_URL);

	private final Rol rol;
	private final String url;

	PaginaDeInicio(Rol rol, String url) {
		this.rol = rol;
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public static String de(Rol rol) {
		for (PaginaDeInicio pagina : values()) {
			if (pagina.rol == rol) {
				return pagina.url;
			}
		}
		throw new IllegalStateException("El rol " + rol + " no tiene pagina de inicio");
	}
}
