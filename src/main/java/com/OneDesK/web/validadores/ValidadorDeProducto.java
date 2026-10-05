package com.OneDesK.web.validadores;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import com.OneDesK.web.formularios.FormularioDeProducto;

/** Un producto del catalogo: su genetica y un precio por gramo. */
@Component
public class ValidadorDeProducto extends ValidadorDeFormulario<FormularioDeProducto> {

	@Override
	public boolean supports(Class<?> clase) {
		return FormularioDeProducto.class.equals(clase);
	}

	@Override
	protected void revisar(FormularioDeProducto form, Errors errores) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errores, "genetica", "validacion.genetica.vacia");
		if (form.getPrecio() < 1) {
			errores.rejectValue("precio", "validacion.precio");
		}
	}
}
