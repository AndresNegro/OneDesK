package com.OneDesK.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Eclipse compila sin guardar los nombres de los parametros (la opcion -parameters que si usa Maven).
 * Sin ellos, Spring no sabe que el campo "email" del formulario va en el parametro email, y todos los
 * formularios fallan al correr la aplicacion desde Eclipse, aunque los tests pasen con Maven.
 * Por eso cada @RequestParam y @PathVariable lleva su nombre escrito, y este test lo controla.
 */
public class NombresDeParametrosTest {

	private static final List<Class<?>> CONTROLLERS = List.of(AccesoController.class, CatalogoController.class,
			MisComprasController.class, EmpleadoController.class, AdminController.class);

	// Todos los parametros que vienen de un formulario o de la direccion tienen su nombre escrito
	@Test
	public void todosLosParametrosTienenSuNombreEscrito() {
		List<String> sinNombre = new ArrayList<>();
		for (Class<?> controller : CONTROLLERS) {
			for (Method metodo : controller.getDeclaredMethods()) {
				for (Parameter parametro : metodo.getParameters()) {
					if (!tieneNombre(parametro)) {
						sinNombre.add(controller.getSimpleName() + "." + metodo.getName() + ": " + parametro.getType().getSimpleName());
					}
				}
			}
		}

		assertTrue(sinNombre.isEmpty(), "Parametros sin nombre escrito: " + sinNombre);
	}

	private boolean tieneNombre(Parameter parametro) {
		RequestParam requestParam = parametro.getAnnotation(RequestParam.class);
		if (requestParam != null) {
			return !requestParam.value().isEmpty() || !requestParam.name().isEmpty();
		}
		PathVariable pathVariable = parametro.getAnnotation(PathVariable.class);
		if (pathVariable != null) {
			return !pathVariable.value().isEmpty() || !pathVariable.name().isEmpty();
		}
		return true;
	}
}
