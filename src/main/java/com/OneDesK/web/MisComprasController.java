package com.OneDesK.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.OneDesK.services.CompraService;
import com.OneDesK.services.UsuarioService;

import jakarta.servlet.http.HttpSession;

/** Las compras del usuario: ver el historial, pagar una impaga o anularla. */
@Controller
public class MisComprasController {

	@Autowired
	private CompraService compraService;
	@Autowired
	private UsuarioService usuarioService;

	@GetMapping("/mis-compras")
	public String misCompras(HttpSession sesion, Model modelo) {
		int usuarioId = Sesion.personaId(sesion);
		modelo.addAttribute("usuario", usuarioService.buscar(usuarioId));
		modelo.addAttribute("compras", compraService.comprasDe(usuarioId));
		modelo.addAttribute("enCarrito", Sesion.carrito(sesion).unidades());
		return "mis-compras";
	}

	// el id de la compra viene de la pagina: el service verifica que sea del usuario de la sesion
	@PostMapping("/mis-compras/{compraId}/pagar")
	public String pagar(@PathVariable int compraId, HttpSession sesion, RedirectAttributes flash) {
		compraService.registrarPagoDe(Sesion.personaId(sesion), compraId);
		flash.addFlashAttribute("exito", "Pagaste la compra #" + compraId);
		return "redirect:/mis-compras";
	}

	@PostMapping("/mis-compras/{compraId}/anular")
	public String anular(@PathVariable int compraId, HttpSession sesion, RedirectAttributes flash) {
		compraService.anularCompraDe(Sesion.personaId(sesion), compraId);
		flash.addFlashAttribute("exito", "Anulaste la compra #" + compraId + " y el stock volvió al catálogo");
		return "redirect:/mis-compras";
	}
}
