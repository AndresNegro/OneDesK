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

import com.OneDesK.services.UsuarioService;
import com.OneDesK.web.formularios.FormularioDeRegistro;
import com.OneDesK.web.validadores.ValidadorDeRegistro;

import jakarta.servlet.http.HttpSession;

/**
 * Ingresar, pedir una cuenta y salir. Son las unicas paginas que se ven sin sesion.
 *
 * El formulario de ingreso no se recibe aca: lo procesa Spring Security en /ingresar, y lo mismo
 * salir en /salir (ver ConfiguracionDeSeguridad). Lo que queda es mostrar la pagina y el motivo
 * cuando alguien no pudo entrar.
 */
@Controller
public class AccesoController extends BaseController {

	public static final String LOGIN_URL = "/";
	public static final String INGRESAR_URL = "/ingresar";
	public static final String REGISTRO_URL = "/registro";
	public static final String SALIR_URL = "/salir";

	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private ValidadorDeRegistro validadorDeRegistro;

	/** Los validadores que revisan los formularios de esta pagina, antes de que Spring los llene. */
	@InitBinder(value = FORM_ATTRIBUTE)
	public void engancharValidadores(WebDataBinder binder) {
		enganchar(binder, validadorDeRegistro);
	}

	// con la sesion abierta, el login lleva directo a la pagina de inicio de su rol
	@GetMapping(value = LOGIN_URL)
	public String login(HttpSession sesion) {
		if (Sesion.ingreso(sesion)) {
			return redirect(PaginaDeInicio.de(Sesion.rol(sesion)));
		}
		return "index";
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
}
