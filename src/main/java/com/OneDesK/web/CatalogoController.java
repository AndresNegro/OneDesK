package com.OneDesK.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.OneDesK.modelo.Compra;
import com.OneDesK.modelo.Producto;
import com.OneDesK.modelo.Usuario;
import com.OneDesK.services.CompraService;
import com.OneDesK.services.LimitesDeCompraService;
import com.OneDesK.services.ProductoService;
import com.OneDesK.services.UsuarioService;

import com.OneDesK.web.formularios.FormularioDeCarrito;

import jakarta.servlet.http.HttpSession;

/** El circuito del usuario: ver el catalogo, armar el carrito y confirmar la compra. */
@Controller
public class CatalogoController extends BaseController {

	public static final String CATALOGO_URL = "/catalogo";
	public static final String CARRITO_URL = "/carrito";
	public static final String AGREGAR_URL = CARRITO_URL + "/agregar";
	public static final String CANTIDAD_URL = CARRITO_URL + "/cantidad";
	public static final String QUITAR_URL = CARRITO_URL + "/quitar";
	public static final String CONFIRMAR_URL = CARRITO_URL + "/confirmar";

	@Autowired
	private ProductoService productoService;
	@Autowired
	private Mensajes mensajes;
	@Autowired
	private CompraService compraService;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private LimitesDeCompraService limitesDeCompra;

	@GetMapping(value = CATALOGO_URL)
	public String catalogo(@RequestParam(name = "busqueda", required = false) String busqueda,
			@RequestParam(name = "orden", required = false) String orden, HttpSession sesion, Model modelo) {
		List<Producto> productos;
		if (busqueda != null && !busqueda.isBlank()) {
			productos = productoService.buscarPorGenetica(busqueda);
		} else if ("precio".equals(orden)) {
			productos = productoService.listarPorPrecio();
		} else {
			productos = productoService.listarProductos();
		}
		// la barra de stock de cada card se mide contra el producto con mas stock
		int stockMaximo = 1;
		for (Producto producto : productos) {
			stockMaximo = Math.max(stockMaximo, producto.getStock());
		}
		modelo.addAttribute("productos", productos);
		modelo.addAttribute("stockMaximo", stockMaximo);
		modelo.addAttribute("busqueda", busqueda);
		modelo.addAttribute("orden", orden);
		modelo.addAttribute("enCarrito", Sesion.carrito(sesion).unidades());
		modelo.addAttribute("limites", limitesDeCompra.obtener());
		return "catalogo";
	}

	@PostMapping(value = AGREGAR_URL)
	public String agregar(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeCarrito form, HttpSession sesion,
			Locale idioma, RedirectAttributes flash) {
		Producto producto = productoService.buscar(form.getProductoId());
		Sesion.carrito(sesion).agregar(form.getProductoId(), form.getCantidad());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.carrito", idioma, form.getCantidad(), producto.getGenetica()));
		return redirect(CATALOGO_URL);
	}

	@GetMapping(value = CARRITO_URL)
	public String carrito(HttpSession sesion, Model modelo) {
		Usuario usuario = usuarioService.buscar(Sesion.personaId(sesion));
		List<LineaVista> lineas = new ArrayList<>();
		int total = 0;
		for (Carrito.Linea linea : Sesion.carrito(sesion).getLineas()) {
			Producto producto = productoService.buscar(linea.getProductoId());
			LineaVista lineaVista = new LineaVista(producto, linea.getCantidad());
			lineas.add(lineaVista);
			total += lineaVista.getSubtotal();
		}
		modelo.addAttribute("lineas", lineas);
		modelo.addAttribute("total", total);
		modelo.addAttribute("usuario", usuario);
		modelo.addAttribute("deudaSiNoPaga", usuario.deudaComprometida() + total);
		modelo.addAttribute("enCarrito", Sesion.carrito(sesion).unidades());
		modelo.addAttribute("gramos", Sesion.carrito(sesion).unidades());
		modelo.addAttribute("limites", limitesDeCompra.obtener());
		return "carrito";
	}

	@PostMapping(value = CANTIDAD_URL)
	public String cambiarCantidad(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeCarrito form, HttpSession sesion) {
		Sesion.carrito(sesion).cambiarCantidad(form.getProductoId(), form.getCantidad());
		return redirect(CARRITO_URL);
	}

	@PostMapping(value = QUITAR_URL)
	public String quitar(@RequestParam("productoId") int productoId, HttpSession sesion) {
		Sesion.carrito(sesion).quitar(productoId);
		return redirect(CARRITO_URL);
	}

	// confirmar manda el pedido a la administracion. El carrito se vacia solo si el pedido salio bien:
	// si una regla lo rechaza (stock, tope, gramos), sigue ahi para corregirlo
	@PostMapping(value = CONFIRMAR_URL)
	public String confirmar(@RequestParam("pago") String pago, HttpSession sesion, Locale idioma, RedirectAttributes flash) {
		Carrito carrito = Sesion.carrito(sesion);
		Compra compra = compraService.realizarCompra(Sesion.personaId(sesion), carrito.comoLineasDeCompra(),
				"ahora".equals(pago));
		carrito.vaciar();
		flash.addFlashAttribute(EXITO, mensajes.de("exito.pedido", idioma, compra.getId()));
		return redirect(MisComprasController.MIS_COMPRAS_URL);
	}

	/** Un producto del carrito con su cantidad, listo para mostrar. */
	public static class LineaVista {

		private final Producto producto;
		private final int cantidad;

		LineaVista(Producto producto, int cantidad) {
			this.producto = producto;
			this.cantidad = cantidad;
		}

		public Producto getProducto() { return producto; }
		public int getCantidad() { return cantidad; }
		public int getSubtotal() { return producto.getPrecio() * cantidad; }
	}
}
