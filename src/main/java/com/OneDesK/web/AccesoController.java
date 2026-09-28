package com.OneDesK.web;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.OneDesK.excepciones.DatoInvalidoException;
import com.OneDesK.modelo.Persona;
import com.OneDesK.services.AccesoService;
import com.OneDesK.services.UsuarioService;
import com.OneDesK.web.formularios.FormularioDeIngreso;
import com.OneDesK.web.formularios.FormularioDeRegistro;

import jakarta.servlet.http.HttpSession;

/** Ingresar, pedir una cuenta y salir. Son las unicas paginas que se ven sin sesion. */
@Controller
public class AccesoController extends BaseController {

	public static final String LOGIN_URL = "/";
	public static final String INGRESAR_URL = "/ingresar";
	public static final String REGISTRO_URL = "/registro";
	public static final String SALIR_URL = "/salir";

	@Autowired
	private AccesoService accesoService;
	@Autowired
	private Mensajes mensajes;
	@Autowired
	private UsuarioService usuarioService;

	// con la sesion abierta, el login lleva directo a la pagina de inicio de su rol
	@GetMapping(value = LOGIN_URL)
	public String login(HttpSession sesion, Model modelo) {
		if (Sesion.ingreso(sesion)) {
			return redirect(Sesion.rol(sesion).getInicio());
		}
		// el formulario vacio es el que la pagina completa con th:field
		modelo.addAttribute(FORM_ATTRIBUTE, new FormularioDeIngreso());
		return "index";
	}

	@PostMapping(value = INGRESAR_URL)
	public String ingresar(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeIngreso form, HttpSession sesion) {
		Persona persona = accesoService.ingresar(form.getEmail(), form.getContrasenia());
		Sesion.iniciar(sesion, persona);
		return redirect(Sesion.rol(sesion).getInicio());
	}

	@GetMapping(value = REGISTRO_URL)
	public String registro(Model modelo) {
		modelo.addAttribute(FORM_ATTRIBUTE, new FormularioDeRegistro());
		return "registro";
	}

	// registrarse es pedir una cuenta: queda pendiente hasta que la administracion la apruebe
	@PostMapping(value = REGISTRO_URL)
	public String registrarse(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeRegistro form, Locale idioma, RedirectAttributes flash) {
		if (!form.getContrasenia().equals(form.getRepetir())) {
			throw new DatoInvalidoException("error.contrasenias.distintas");
		}
		usuarioService.registrar(form.getNombre(), form.getApellido(), form.getEmail(), form.getContrasenia());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.solicitud", idioma));
		return redirect(LOGIN_URL);
	}

	@PostMapping(value = SALIR_URL)
	public String salir(HttpSession sesion) {
		sesion.invalidate();
		return redirect(LOGIN_URL);
	}
}
