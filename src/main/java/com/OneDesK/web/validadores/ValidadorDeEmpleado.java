package com.OneDesK.web.validadores;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import com.OneDesK.web.formularios.FormularioDeEmpleado;

/** El alta de un empleado: sus datos completos y un sueldo. */
@Component
public class ValidadorDeEmpleado extends ValidadorDeFormulario<FormularioDeEmpleado> {

	@Override
	public boolean supports(Class<?> clase) {
		return FormularioDeEmpleado.class.equals(clase);
	}

	@Override
	protected void revisar(FormularioDeEmpleado form, Errors errores) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errores, "nombre", "validacion.nombre.vacio");
		ValidationUtils.rejectIfEmptyOrWhitespace(errores, "apellido", "validacion.apellido.vacio");
		ValidationUtils.rejectIfEmptyOrWhitespace(errores, "email", "validacion.email.vacio");
		ValidationUtils.rejectIfEmptyOrWhitespace(errores, "contrasenia", "validacion.contrasenia.vacia");
		if (form.getSalario() < 1) {
			errores.rejectValue("salario", "validacion.salario");
		}
	}
}
