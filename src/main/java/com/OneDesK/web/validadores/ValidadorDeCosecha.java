package com.OneDesK.web.validadores;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import com.OneDesK.web.formularios.FormularioDeCosecha;

/** Una cosecha tiene que sumar gramos al stock. */
@Component
public class ValidadorDeCosecha extends ValidadorDeFormulario<FormularioDeCosecha> {

	@Override
	public boolean supports(Class<?> clase) {
		return FormularioDeCosecha.class.equals(clase);
	}

	@Override
	protected void revisar(FormularioDeCosecha form, Errors errores) {
		if (form.getCantidad() < 1) {
			errores.rejectValue("cantidad", "validacion.cantidad");
		}
	}
}
