package com.OneDesK.web.validadores;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import com.OneDesK.web.formularios.FormularioDeIngreso;

/** Los dos campos del login tienen que venir completos antes de ir a buscar a la persona. */
@Component
public class ValidadorDeIngreso extends ValidadorDeFormulario<FormularioDeIngreso> {

	@Override
	public boolean supports(Class<?> clase) {
		return FormularioDeIngreso.class.equals(clase);
	}

	@Override
	protected void revisar(FormularioDeIngreso form, Errors errores) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errores, "email", "validacion.email.vacio");
		ValidationUtils.rejectIfEmptyOrWhitespace(errores, "contrasenia", "validacion.contrasenia.vacia");
	}
}
