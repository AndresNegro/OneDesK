package com.OneDesK.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.OneDesK.web.validadores.ValidadorDeFormulario;

/**
 * Lo que comparten todos los controllers: el nombre con el que viaja el formulario a la plantilla,
 * los de los dos avisos, armar la redireccion sin repetir el prefijo en cada return y enganchar
 * los validadores. Un nombre escrito a mano en veinte lugares es un error de tipeo esperando:
 * aca esta escrito una sola vez.
 */
public abstract class BaseController {

	/** Con este nombre la plantilla encuentra el formulario en th:object="${form}". */
	protected static final String FORM_ATTRIBUTE = "form";

	/** Los avisos que muestra el fragmento avisos: en verde y en rosa. */
	protected static final String EXITO = "exito";
	protected static final String ERROR = "error";

	/** Los motivos por los que un formulario no paso: salen todos juntos, uno abajo del otro. */
	protected static final String ERRORES = "errores";

	@Autowired
	protected Mensajes mensajes;

	/**
	 * Le engancha al formulario los validadores que le sirven. Cada controller llama a este metodo
	 * desde su @InitBinder con los suyos: una pagina tiene varios formularios distintos, y Spring
	 * rechaza de entrada un validador que no es para el formulario que esta armando.
	 */
	protected void enganchar(WebDataBinder binder, ValidadorDeFormulario<?>... validadores) {
		for (ValidadorDeFormulario<?> validador : validadores) {
			// cuando todavia no se armo el formulario no se sabe de que tipo va a ser: se enganchan
			// todos, y el que no corresponda se corre al costado solo (ver ValidadorDeFormulario)
			if (binder.getTarget() == null || validador.supports(binder.getTarget().getClass())) {
				binder.addValidators(validador);
			}
		}
	}

	protected String redirect(String ruta) {
		return "redirect:" + ruta;
	}

	/**
	 * Vuelve a la pagina con el primer motivo por el que el formulario no paso, como aviso arriba.
	 * Lo usan los paneles, que tienen muchos formularios juntos y no pueden mostrar el error abajo
	 * del campo: las paginas de un solo formulario lo muestran ahi con th:errors.
	 */
	protected String avisar(BindingResult errores, Locale idioma, RedirectAttributes flash, String ruta) {
		List<String> motivos = new ArrayList<>();
		for (ObjectError error : errores.getAllErrors()) {
			// los errores de tipo (una letra donde va un numero) los pone Spring con su propia clave
			motivos.add(mensajes.deOSino(error.getCode(), "error.campos", idioma, error.getArguments()));
		}
		flash.addFlashAttribute(ERRORES, motivos);
		return redirect(ruta);
	}
}
