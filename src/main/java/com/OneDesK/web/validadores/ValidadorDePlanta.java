package com.OneDesK.web.validadores;

import java.time.LocalDate;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import com.OneDesK.web.formularios.FormularioDePlanta;

/** Una planta nueva: la genetica, las dos fechas en orden y los tres tiempos positivos. */
@Component
public class ValidadorDePlanta extends ValidadorDeFormulario<FormularioDePlanta> {

	@Override
	public boolean supports(Class<?> clase) {
		return FormularioDePlanta.class.equals(clase);
	}

	@Override
	protected void revisar(FormularioDePlanta form, Errors errores) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errores, "genetica", "validacion.genetica.vacia");
		if (form.getFechaPlantado() == null) {
			errores.rejectValue("fechaPlantado", "validacion.fecha.obligatoria");
		} else if (form.getFechaPlantado().isAfter(LocalDate.now())) {
			errores.rejectValue("fechaPlantado", "validacion.plantado.futuro");
		}
		if (form.getFechaGerminado() == null) {
			errores.rejectValue("fechaGerminado", "validacion.fecha.obligatoria");
		} else if (form.getFechaPlantado() != null && form.getFechaGerminado().isAfter(form.getFechaPlantado())) {
			errores.rejectValue("fechaGerminado", "validacion.germinado.tarde");
		}
		rechazarSiNoEsPositivo(errores, "tiempoRegado", form.getTiempoRegado());
		rechazarSiNoEsPositivo(errores, "tiempoLuz", form.getTiempoLuz());
		rechazarSiNoEsPositivo(errores, "tiempoVentilacion", form.getTiempoVentilacion());
	}

	private void rechazarSiNoEsPositivo(Errors errores, String campo, int minutos) {
		if (minutos < 1) {
			errores.rejectValue(campo, "validacion.tiempo");
		}
	}
}
