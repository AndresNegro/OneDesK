package com.OneDesK.web;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.OneDesK.excepciones.DatoInvalidoException;
import com.OneDesK.modelo.Persona;
import com.OneDesK.services.AccesoService;
import com.OneDesK.services.UsuarioService;
import com.OneDesK.web.formularios.FormularioDeIngreso;
import com.OneDesK.web.formularios.FormularioDeRegistro;

import com.OneDesK.web.validadores.ValidadorDeIngreso;
import com.OneDesK.web.validadores.ValidadorDeRegistro;

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
	private UsuarioService usuarioService;
	@Autowired
	private ValidadorDeIngreso validadorDeIngreso;
	@Autowired
	private ValidadorDeRegistro validadorDeRegistro;

	/** Los validadores que revisan los formularios de esta pagina, antes de que Spring los llene. */
	@InitBinder(value = FORM_ATTRIBUTE)
	public void engancharValidadores(WebDataBinder binder) {
		enganchar(binder, validadorDeIngreso, validadorDeRegistro);
	}

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
	public String ingresar(@Validated @ModelAttribute(FORM_ATTRIBUTE) FormularioDeIngreso form,
			BindingResult errores, HttpSession sesion) {
		// la pagina vuelve a mostrarse con el motivo abajo del campo que fallo
		if (errores.hasErrors()) {
			return "index";
		}
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
	public String registrarse(@Validated @ModelAttribute(FORM_ATTRIBUTE) FormularioDeRegistro form,
			BindingResult errores, Locale idioma, RedirectAttributes flash) {
		if (errores.hasErrors()) {
			return "registro";
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
