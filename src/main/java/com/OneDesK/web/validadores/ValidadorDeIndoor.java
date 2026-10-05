package com.OneDesK.web.validadores;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import com.OneDesK.web.formularios.FormularioDeIndoor;

/** Un indoor necesita nombre y lugar para al menos una planta. */
@Component
public class ValidadorDeIndoor extends ValidadorDeFormulario<FormularioDeIndoor> {

	@Override
	public boolean supports(Class<?> clase) {
		return FormularioDeIndoor.class.equals(clase);
	}

	@Override
	protected void revisar(FormularioDeIndoor form, Errors errores) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errores, "nombre", "validacion.nombre.vacio");
		if (form.getCapacidad() < 1) {
			errores.rejectValue("capacidad", "validacion.capacidad");
		}
	}
}
