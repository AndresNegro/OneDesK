package com.OneDesK.web.validadores;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import com.OneDesK.web.formularios.FormularioDeLimites;

/** Los gramos por compra: un minimo de al menos 1 g y un maximo que no quede por debajo. */
@Component
public class ValidadorDeLimites extends ValidadorDeFormulario<FormularioDeLimites> {

	@Override
	public boolean supports(Class<?> clase) {
		return FormularioDeLimites.class.equals(clase);
	}

	@Override
	protected void revisar(FormularioDeLimites form, Errors errores) {
		if (form.getMinimo() < 1) {
			errores.rejectValue("minimo", "validacion.minimo");
		}
		if (form.getMaximo() < form.getMinimo()) {
			errores.rejectValue("maximo", "validacion.maximo");
		}
	}
}
