package com.OneDesK.web;

import java.util.Locale;

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
public class MisComprasController extends BaseController {

	public static final String MIS_COMPRAS_URL = "/mis-compras";
	public static final String PAGAR_URL = MIS_COMPRAS_URL + "/{compraId}/pagar";
	public static final String ANULAR_URL = MIS_COMPRAS_URL + "/{compraId}/anular";

	@Autowired
	private CompraService compraService;
	@Autowired
	private Mensajes mensajes;
	@Autowired
	private UsuarioService usuarioService;

	@GetMapping(value = MIS_COMPRAS_URL)
	public String misCompras(HttpSession sesion, Model modelo) {
		int usuarioId = Sesion.personaId(sesion);
		modelo.addAttribute("usuario", usuarioService.buscar(usuarioId));
		modelo.addAttribute("compras", compraService.comprasDe(usuarioId));
		modelo.addAttribute("enCarrito", Sesion.carrito(sesion).unidades());
		return "mis-compras";
	}

	// el id de la compra viene de la pagina: el service verifica que sea del usuario de la sesion
	@PostMapping(value = PAGAR_URL)
	public String pagar(@PathVariable("compraId") int compraId, HttpSession sesion, Locale idioma, RedirectAttributes flash) {
		compraService.registrarPagoDe(Sesion.personaId(sesion), compraId);
		flash.addFlashAttribute(EXITO, mensajes.de("exito.pago", idioma, compraId));
		return redirect(MIS_COMPRAS_URL);
	}

	@PostMapping(value = ANULAR_URL)
	public String anular(@PathVariable("compraId") int compraId, HttpSession sesion, Locale idioma, RedirectAttributes flash) {
		compraService.anularCompraDe(Sesion.personaId(sesion), compraId);
		flash.addFlashAttribute(EXITO, mensajes.de("exito.anulada", idioma, compraId));
		return redirect(MIS_COMPRAS_URL);
	}
}
