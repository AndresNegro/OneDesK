package com.OneDesK.web.validadores;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * La base de los validadores. Es un Validator de Spring, con el tipo del formulario escrito, asi
 * cada validador recibe el suyo y no tiene que castear nada.
 *
 * Cuando el controller engancha varios validadores, Spring se los pasa todos a cada formulario sin
 * fijarse en supports: por eso el que no es para ese formulario se corre al costado aca, una sola
 * vez para todos.
 *
 * Se revisa lo que se puede mirar del formulario, y lo que haga falta buscar en la base para avisar
 * a tiempo (ver ValidadorDeRegistro). Las reglas del negocio siguen en el modelo, que es el unico
 * que las puede hacer cumplir siempre.
 */
public abstract class ValidadorDeFormulario<T> implements Validator {

	@Override
	@SuppressWarnings("unchecked")
	public final void validate(Object destino, Errors errores) {
		if (destino != null && supports(destino.getClass())) {
			revisar((T) destino, errores);
		}
	}

	/** Lo que mira este validador, ya con el formulario de su tipo. */
	protected abstract void revisar(T formulario, Errors errores);
}
