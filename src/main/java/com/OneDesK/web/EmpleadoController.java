package com.OneDesK.web;

import java.time.LocalDate;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;
import com.OneDesK.services.EmpleadoIndoorService;
import com.OneDesK.services.ProductoService;
import com.OneDesK.services.RegistroProduccionService;

import com.OneDesK.web.formularios.FormularioDeCosecha;
import com.OneDesK.web.formularios.FormularioDeEvento;
import com.OneDesK.web.formularios.FormularioDePlanta;
import com.OneDesK.web.formularios.FormularioDeProducto;

import jakarta.servlet.http.HttpSession;

/** El panel del empleado: atender eventos, plantar y cosechar en los indoors que tiene a cargo. */
@Controller
public class EmpleadoController extends BaseController {

	public static final String EMPLEADO_URL = "/empleado";
	public static final String ATENDER_URL = EMPLEADO_URL + "/atender";
	public static final String PLANTAR_URL = EMPLEADO_URL + "/plantar";
	public static final String COSECHAR_URL = EMPLEADO_URL + "/cosechar";
	public static final String PRODUCTOS_URL = EMPLEADO_URL + "/productos";

	@Autowired
	private EmpleadoIndoorService empleadoService;
	@Autowired
	private Mensajes mensajes;
	@Autowired
	private RegistroProduccionService registroProduccionService;
	@Autowired
	private ProductoService productoService;

	@GetMapping(value = EMPLEADO_URL)
	public String panel(HttpSession sesion, Model modelo) {
		int empleadoId = Sesion.personaId(sesion);
		EmpleadoIndoor empleado = empleadoService.buscar(empleadoId);
		modelo.addAttribute("empleado", empleado);
		modelo.addAttribute("pendientes", empleadoService.eventosPendientes(empleadoId));
		modelo.addAttribute("enCultivo", empleado.plantasEnCultivo());
		modelo.addAttribute("hoy", LocalDate.now());
		return "empleado";
	}

	@PostMapping(value = ATENDER_URL)
	public String atender(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeEvento form, HttpSession sesion,
			Locale idioma, RedirectAttributes flash) {
		empleadoService.atenderEvento(Sesion.personaId(sesion), form.getIndoorId(), form.getEventoId());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.evento", idioma));
		return redirect(EMPLEADO_URL);
	}

	@PostMapping(value = PLANTAR_URL)
	public String plantar(@ModelAttribute(FORM_ATTRIBUTE) FormularioDePlanta form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		Planta planta = new Planta(form.getGenetica(), form.getFechaPlantado(), form.getFechaGerminado(),
				form.getTiempoRegado(), form.getTiempoLuz(), form.getTiempoVentilacion());
		empleadoService.plantar(Sesion.personaId(sesion), form.getIndoorId(), planta);
		flash.addFlashAttribute(EXITO, mensajes.de("exito.plantada", idioma, planta.getGenetica(), planta.getIndoor().getNombre()));
		return redirect(EMPLEADO_URL);
	}

	// el formulario manda solo la planta: el indoor se busca entre los que el empleado tiene a cargo
	@PostMapping(value = COSECHAR_URL)
	public String cosechar(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeCosecha form, HttpSession sesion, Locale idioma,
			RedirectAttributes flash) {
		int empleadoId = Sesion.personaId(sesion);
		Indoor indoor = indoorDeLaPlanta(empleadoService.buscar(empleadoId), form.getPlantaId());
		registroProduccionService.registrarCosecha(empleadoId, indoor.getId(), form.getPlantaId(), form.getCantidad());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.cosecha", idioma, form.getCantidad()));
		return redirect(EMPLEADO_URL);
	}

	@PostMapping(value = PRODUCTOS_URL)
	public String altaProducto(@ModelAttribute(FORM_ATTRIBUTE) FormularioDeProducto form, Locale idioma, RedirectAttributes flash) {
		productoService.crearProducto(form.getGenetica(), form.getPrecio());
		flash.addFlashAttribute(EXITO, mensajes.de("exito.producto", idioma, form.getGenetica().trim()));
		return redirect(EMPLEADO_URL);
	}

	private Indoor indoorDeLaPlanta(EmpleadoIndoor empleado, int plantaId) {
		Indoor indoor = empleado.indoorDeLaPlanta(plantaId);
		if (indoor == null) {
			throw new RecursoNoEncontradoException("La planta " + plantaId + " no está en tus indoors");
		}
		return indoor;
	}
}
