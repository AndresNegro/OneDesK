package com.OneDesK.web.validadores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import com.OneDesK.repositories.PersonaRepository;
import com.OneDesK.web.formularios.FormularioDeRegistro;

/**
 * La solicitud de cuenta: los datos completos, las dos contrasenias iguales y un email que no este
 * tomado. Para eso ultimo el validador consulta la base: no todo se puede mirar del formulario solo.
 */
@Component
public class ValidadorDeRegistro extends ValidadorDeFormulario<FormularioDeRegistro> {

	private static final int CONTRASENIA_MINIMA = 5;

	@Autowired
	private PersonaRepository personas;

	@Override
	public boolean supports(Class<?> clase) {
		return FormularioDeRegistro.class.equals(clase);
	}

	@Override
	protected void revisar(FormularioDeRegistro form, Errors errores) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errores, "nombre", "validacion.nombre.vacio");
		ValidationUtils.rejectIfEmptyOrWhitespace(errores, "apellido", "validacion.apellido.vacio");
		ValidationUtils.rejectIfEmptyOrWhitespace(errores, "email", "validacion.email.vacio");
		ValidationUtils.rejectIfEmptyOrWhitespace(errores, "contrasenia", "validacion.contrasenia.vacia");
		if (form.getContrasenia() != null && form.getContrasenia().length() < CONTRASENIA_MINIMA) {
			errores.rejectValue("contrasenia", "validacion.contrasenia.corta", new Object[] { CONTRASENIA_MINIMA }, null);
		}
		// se compara aca y no en el modelo: el repetir es del formulario, el Usuario ni lo conoce
		if (form.getContrasenia() != null && !form.getContrasenia().equals(form.getRepetir())) {
			errores.rejectValue("repetir", "error.contrasenias.distintas");
		}
		// el aviso sale abajo del campo, antes de intentar guardar. El email unico igual lo
		// sigue cuidando el service: dos personas podrian pedir la misma cuenta a la vez
		if (!errores.hasFieldErrors("email") && personas.existsByEmail(form.getEmail().trim().toLowerCase())) {
			errores.rejectValue("email", "validacion.email.tomado");
		}
	}
}
