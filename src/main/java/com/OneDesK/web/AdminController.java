package com.OneDesK.web;

import java.time.LocalDate;
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

import com.OneDesK.evento.Evento;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;
import com.OneDesK.modelo.Usuario;
import com.OneDesK.services.AdministradorService;

import com.OneDesK.web.formularios.FormularioDeAjuste;
import com.OneDesK.web.formularios.FormularioDeAsignacion;
import com.OneDesK.web.formularios.FormularioDeEmpleado;
import com.OneDesK.web.formularios.FormularioDeIndoor;
import com.OneDesK.web.formularios.FormularioDeLimites;
import com.OneDesK.web.formularios.FormularioDePlanta;
import com.OneDesK.web.formularios.FormularioDePrecio;
import com.OneDesK.web.formularios.FormularioDeProducto;
import com.OneDesK.web.formularios.FormularioDeSalario;
import com.OneDesK.web.formularios.FormularioDeStock;
import com.OneDesK.web.formularios.FormularioDeTope;

import jakarta.servlet.http.HttpSession;

/**
 * La pagina de administracion. Cada accion vuelve a la pestana desde la que se hizo (el #seccion
 * de la direccion), asi no hay que buscarla de nuevo.
 */
@Controller
public class AdminController extends BaseController {

	public static final String ADMIN_URL = "/admin";
	public static final String CREAR_INDOOR_URL = ADMIN_URL + "/indoors";
	public static final String EDITAR_INDOOR_URL = ADMIN_URL + "/indoors/editar";
	public static final String PLANTAR_URL = ADMIN_URL + "/plantar";
	public static final String EMPLEADOS_URL = ADMIN_URL + "/empleados";
	public static final String ASIGNAR_URL = ADMIN_URL + "/empleados/asignar";
	public static final String DESASIGNAR_URL = ADMIN_URL + "/empleados/desasignar";
	public static final String SALARIO_URL = ADMIN_URL + "/empleados/salario";
	public static final String PRODUCTOS_URL = ADMIN_URL + "/productos";
	public static final String PRECIO_URL = ADMIN_URL + "/productos/precio";
	public static final String FIJAR_STOCK_URL = ADMIN_URL + "/productos/fijar";
	public static final String AJUSTAR_STOCK_URL = ADMIN_URL + "/productos/ajustar";
	public static final String APROBAR_SOLICITUD_URL = ADMIN_URL + "/solicitudes/aprobar";
	public static final String RECHAZAR_SOLICITUD_URL = ADMIN_URL + "/solicitudes/rechazar";
	public static final String TOPE_URL = ADMIN_URL + "/usuarios/tope";
	public static final String ANULAR_COMPRA_URL = ADMIN_URL + "/compras/anular";
	public static final String APROBAR_COMPRA_URL = ADMIN_URL + "/compras/aprobar";
	public static final String RECHAZAR_COMPRA_URL = ADMIN_URL + "/compras/rechazar";
	public static final String LIMITES_URL = ADMIN_URL + "/limites";

	// cada accion vuelve a la pestana desde la que se hizo
	private static final String SECCION_INDOORS = ADMIN_URL + "#indoors";
	private static final String SECCION_PRODUCTOS = ADMIN_URL + "#productos";
	private static final String SECCION_USUARIOS = ADMIN_URL + "#usuarios";
	private static final String SECCION_COMPRAS = ADMIN_URL + "#compras";

	@Autowired
	private AdministradorService admin;
	@Autowired
	private Mensajes mensajes;

	@GetMapping(value = ADMIN_URL)
	public String panel(HttpSession sesion, Model modelo) {
		int adminId = Sesion.personaId(sesion);
		List<Indoor> indoors = admin.indoors(adminId);
		List<EmpleadoIndoor> empleados = admin.empleados(adminId);
		List<Usuario> deudores = admin.deudores(adminId);
		List<Evento> pendientes = admin.eventosPendientes(adminId);

		modelo.addAttribute("indoors", indoors);
		modelo.addAttribute("empleados", empleados);
		modelo.addAttribute("productos", admin.productos(adminId));
		modelo.addAttribute("usuarios", admin.usuarios(adminId));
		modelo.addAttribute("solicitudes", admin.solicitudesPendientes(adminId));
		modelo.addAttribute("comprasImpagas", admin.comprasImpagas(adminId));
		modelo.addAttribute("comprasPendientes", admin.comprasPendientes(adminId));
		modelo.addAttribute("limites", admin.limitesDeCompra(adminId));
		modelo.addAttribute("registrosDeProduccion", admin.registrosDeProduccion(adminId));
		modelo.addAttribute("registrosDePago", admin.registrosDePago(adminId));
		modelo.addAttribute("deudores", deudores);
		modelo.addAttribute("pendientes", pendientes);
		modelo.addAttribute("hoy", LocalDate.now());
		modelo.addAttribute("nombre", Sesion.nombre(sesion));

		// numeros del resumen de arriba
		int plantasEnCultivo = 0;
		for (Indoor indoor : indoors) {
			plantasEnCultivo += indoor.plantasEnCultivo();
		}
		int sinIndoor = 0;
		for (EmpleadoIndoor empleado : empleados) {
			if (empleado.estaSinIndoor()) {
				sinIndoor++;
			}
		}
		int deudaTotal = 0;
		for (Usuario deudor : deudores) {
			deudaTotal += deudor.montoDeDeuda();
		}
		modelo.addAttribute("plantasEnCultivo", plantasEnCultivo);
		modelo.addAttribute("empleadosSinIndoor", sinIndoor);
		modelo.addAttribute("deudaTotal", deudaTotal);
		return "admin";
	}

	// --- indoors y empleados ---

	@PostMapping(value = CREAR_INDOOR_URL)
	public String crearIndoor(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeIndoor form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		Indoor indoor = admin.crearIndoor(Sesion.personaId(sesion), form.getNombre(), form.getCapacidad());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.indoor.creado", idioma, indoor.getNombre()));
		return redirect(SECCION_INDOORS);
	}

	@PostMapping(value = EDITAR_INDOOR_URL)
	public String editarIndoor(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeIndoor form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		Indoor indoor = admin.editarIndoor(Sesion.personaId(sesion), form.getIndoorId(), form.getNombre(),
				form.getCapacidad());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.indoor.editado", idioma, indoor.getNombre()));
		return redirect(SECCION_INDOORS);
	}

	@PostMapping(value = PLANTAR_URL)
	public String plantar(@ModelAttribute(FORM_ATTRIBUTE) FormularioDePlanta form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		Planta planta = new Planta(form.getGenetica(), form.getFechaPlantado(), form.getFechaGerminado(),
				form.getTiempoRegado(), form.getTiempoLuz(), form.getTiempoVentilacion());
		admin.plantar(Sesion.personaId(sesion), form.getIndoorId(), planta);
		flash.addFlashAttribute(EXITO, mensajes.de("exito.plantada", idioma, planta.getGenetica(), planta.getIndoor().getNombre()));
		return redirect(SECCION_INDOORS);
	}

	@PostMapping(value = EMPLEADOS_URL)
	public String registrarEmpleado(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeEmpleado form, HttpSession sesion,
			Locale idioma, RedirectAttributes flash) {
		admin.registrarEmpleado(Sesion.personaId(sesion), form.getNombre(), form.getApellido(), form.getEmail(),
				form.getContrasenia(), form.getSalario());
		flash.addFlashAttribute(EXITO,
				mensajes.de("exito.empleado", idioma, form.getNombre().trim() + " " + form.getApellido().trim()));
		return redirect(SECCION_INDOORS);
	}

	@PostMapping(value = ASIGNAR_URL)
	public String asignar(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeAsignacion form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		admin.asignarEmpleado(Sesion.personaId(sesion), form.getEmpleadoId(), form.getIndoorId());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.asignado", idioma));
		return redirect(SECCION_INDOORS);
	}

	@PostMapping(value = DESASIGNAR_URL)
	public String desasignar(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeAsignacion form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		admin.desasignarEmpleado(Sesion.personaId(sesion), form.getEmpleadoId(), form.getIndoorId());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.desasignado", idioma));
		return redirect(SECCION_INDOORS);
	}

	@PostMapping(value = SALARIO_URL)
	public String salario(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeSalario form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		admin.cambiarSalario(Sesion.personaId(sesion), form.getEmpleadoId(), form.getSalario());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.salario", idioma));
		return redirect(SECCION_INDOORS);
	}

	// --- productos ---

	@PostMapping(value = PRODUCTOS_URL)
	public String crearProducto(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeProducto form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		admin.crearProducto(Sesion.personaId(sesion), form.getGenetica(), form.getPrecio());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.producto", idioma, form.getGenetica().trim()));
		return redirect(SECCION_PRODUCTOS);
	}

	@PostMapping(value = PRECIO_URL)
	public String precio(@ModelAttribute(FORM_ATTRIBUTE) FormularioDePrecio form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		admin.cambiarPrecio(Sesion.personaId(sesion), form.getProductoId(), form.getPrecio());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.precio", idioma));
		return redirect(SECCION_PRODUCTOS);
	}

	@PostMapping(value = FIJAR_STOCK_URL)
	public String fijarStock(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeStock form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		admin.fijarStock(Sesion.personaId(sesion), form.getProductoId(), form.getStock());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.stock.fijado", idioma, form.getStock()));
		return redirect(SECCION_PRODUCTOS);
	}

	@PostMapping(value = AJUSTAR_STOCK_URL)
	public String ajustarStock(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeAjuste form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		admin.ajustarStock(Sesion.personaId(sesion), form.getProductoId(), form.getCantidad());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.stock.ajustado", idioma));
		return redirect(SECCION_PRODUCTOS);
	}

	// --- usuarios y compras ---

	@PostMapping(value = APROBAR_SOLICITUD_URL)
	public String aprobar(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeTope form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		Usuario usuario = admin.aprobarUsuario(Sesion.personaId(sesion), form.getUsuarioId(), form.getTope());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.usuario.aprobado", idioma, usuario.getNombre() + " " + usuario.getApellido()));
		return redirect(SECCION_USUARIOS);
	}

	@PostMapping(value = RECHAZAR_SOLICITUD_URL)
	public String rechazar(@RequestParam("usuarioId") int usuarioId, HttpSession sesion, Locale idioma, RedirectAttributes flash) {
		admin.rechazarUsuario(Sesion.personaId(sesion), usuarioId);
		flash.addFlashAttribute(EXITO, mensajes.de("exito.solicitud.rechazada", idioma));
		return redirect(SECCION_USUARIOS);
	}

	@PostMapping(value = TOPE_URL)
	public String tope(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeTope form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		admin.asignarTope(Sesion.personaId(sesion), form.getUsuarioId(), form.getTope());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.tope", idioma));
		return redirect(SECCION_USUARIOS);
	}

	@PostMapping(value = ANULAR_COMPRA_URL)
	public String anular(@RequestParam("compraId") int compraId, HttpSession sesion, Locale idioma, RedirectAttributes flash) {
		admin.anularCompra(Sesion.personaId(sesion), compraId);
		flash.addFlashAttribute(EXITO, mensajes.de("exito.compra.anulada", idioma, compraId));
		return redirect(SECCION_COMPRAS);
	}

	@PostMapping(value = APROBAR_COMPRA_URL)
	public String aprobarCompra(@RequestParam("compraId") int compraId, HttpSession sesion, Locale idioma, RedirectAttributes flash) {
		admin.aprobarCompra(Sesion.personaId(sesion), compraId);
		flash.addFlashAttribute(EXITO, mensajes.de("exito.compra.aprobada", idioma, compraId));
		return redirect(SECCION_COMPRAS);
	}

	@PostMapping(value = RECHAZAR_COMPRA_URL)
	public String rechazarCompra(@RequestParam("compraId") int compraId, HttpSession sesion, Locale idioma, RedirectAttributes flash) {
		admin.rechazarCompra(Sesion.personaId(sesion), compraId);
		flash.addFlashAttribute(EXITO, mensajes.de("exito.compra.rechazada", idioma, compraId));
		return redirect(SECCION_COMPRAS);
	}

	@PostMapping(value = LIMITES_URL)
	public String limites(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeLimites form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		admin.cambiarLimites(Sesion.personaId(sesion), form.getMinimo(), form.getMaximo());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.limites", idioma, form.getMinimo(), form.getMaximo()));
		return redirect(SECCION_COMPRAS);
	}
}
