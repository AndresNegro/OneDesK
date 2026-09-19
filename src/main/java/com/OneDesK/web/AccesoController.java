package com.OneDesK.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.OneDesK.modelo.Persona;
import com.OneDesK.services.AccesoService;
import com.OneDesK.services.UsuarioService;

import jakarta.servlet.http.HttpSession;

/** Ingresar, pedir una cuenta y salir. Son las unicas paginas que se ven sin sesion. */
@Controller
public class AccesoController {

	@Autowired
	private AccesoService accesoService;
	@Autowired
	private UsuarioService usuarioService;

	// con la sesion abierta, el login lleva directo a la pagina de inicio de su rol
	@GetMapping("/")
	public String login(HttpSession sesion) {
		if (Sesion.ingreso(sesion)) {
			return "redirect:" + Sesion.rol(sesion).getInicio();
		}
		return "index";
	}

	@PostMapping("/ingresar")
	public String ingresar(@RequestParam String email, @RequestParam String contrasenia, HttpSession sesion) {
		Persona persona = accesoService.ingresar(email, contrasenia);
		Sesion.iniciar(sesion, persona);
		return "redirect:" + Sesion.rol(sesion).getInicio();
	}

	@GetMapping("/registro")
	public String registro() {
		return "registro";
	}

	// registrarse es pedir una cuenta: queda pendiente hasta que la administracion la apruebe
	@PostMapping("/registro")
	public String registrarse(@RequestParam String nombre, @RequestParam String apellido,
			@RequestParam String email, @RequestParam String contrasenia, @RequestParam String repetir,
			RedirectAttributes flash) {
		if (!contrasenia.equals(repetir)) {
			throw new IllegalArgumentException("Las contraseñas no coinciden");
		}
		usuarioService.registrar(nombre, apellido, email, contrasenia);
		flash.addFlashAttribute("exito",
				"Recibimos tu solicitud. Te vamos a avisar por mail cuando la administración la apruebe.");
		return "redirect:/";
	}

	@PostMapping("/salir")
	public String salir(HttpSession sesion) {
		sesion.invalidate();
		return "redirect:/";
	}
}
