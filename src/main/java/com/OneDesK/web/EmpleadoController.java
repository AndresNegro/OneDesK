package com.OneDesK.web;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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

import jakarta.servlet.http.HttpSession;

/** El panel del empleado: atender eventos, plantar y cosechar en los indoors que tiene a cargo. */
@Controller
public class EmpleadoController {

	@Autowired
	private EmpleadoIndoorService empleadoService;
	@Autowired
	private RegistroProduccionService registroProduccionService;
	@Autowired
	private ProductoService productoService;

	@GetMapping("/empleado")
	public String panel(HttpSession sesion, Model modelo) {
		int empleadoId = Sesion.personaId(sesion);
		EmpleadoIndoor empleado = empleadoService.buscar(empleadoId);
		List<Planta> enCultivo = new ArrayList<>();
		for (Indoor indoor : empleado.getSectoresACargo()) {
			for (Planta planta : indoor.getPlantas()) {
				if (!planta.isCosechada()) {
					enCultivo.add(planta);
				}
			}
		}
		modelo.addAttribute("empleado", empleado);
		modelo.addAttribute("pendientes", empleadoService.eventosPendientes(empleadoId));
		modelo.addAttribute("enCultivo", enCultivo);
		modelo.addAttribute("hoy", LocalDate.now());
		return "empleado";
	}

	@PostMapping("/empleado/atender")
	public String atender(@RequestParam("indoorId") int indoorId, @RequestParam("eventoId") int eventoId, HttpSession sesion,
			RedirectAttributes flash) {
		empleadoService.atenderEvento(Sesion.personaId(sesion), indoorId, eventoId);
		flash.addFlashAttribute("exito", "Evento atendido");
		return "redirect:/empleado";
	}

	@PostMapping("/empleado/plantar")
	public String plantar(@RequestParam("indoorId") int indoorId, @RequestParam("genetica") String genetica,
			@RequestParam("fechaGerminado") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaGerminado,
			@RequestParam("fechaPlantado") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaPlantado,
			@RequestParam("tiempoRegado") int tiempoRegado, @RequestParam("tiempoLuz") int tiempoLuz, @RequestParam("tiempoVentilacion") int tiempoVentilacion,
			HttpSession sesion, RedirectAttributes flash) {
		Planta planta = new Planta(genetica, fechaPlantado, fechaGerminado, tiempoRegado, tiempoLuz,
				tiempoVentilacion);
		empleadoService.plantar(Sesion.personaId(sesion), indoorId, planta);
		flash.addFlashAttribute("exito", "Plantaste " + planta.getGenetica() + " en " + planta.getIndoor().getNombre());
		return "redirect:/empleado";
	}

	// el formulario manda solo la planta: el indoor se busca entre los que el empleado tiene a cargo
	@PostMapping("/empleado/cosechar")
	public String cosechar(@RequestParam("plantaId") int plantaId, @RequestParam("cantidad") int cantidad, HttpSession sesion,
			RedirectAttributes flash) {
		int empleadoId = Sesion.personaId(sesion);
		Indoor indoor = indoorDeLaPlanta(empleadoService.buscar(empleadoId), plantaId);
		registroProduccionService.registrarCosecha(empleadoId, indoor.getId(), plantaId, cantidad);
		flash.addFlashAttribute("exito", "Cosecha registrada: " + cantidad + " g al stock");
		return "redirect:/empleado";
	}

	@PostMapping("/empleado/productos")
	public String altaProducto(@RequestParam("genetica") String genetica, @RequestParam("precio") int precio, RedirectAttributes flash) {
		productoService.crearProducto(genetica, precio);
		flash.addFlashAttribute("exito", "Diste de alta " + genetica.trim());
		return "redirect:/empleado";
	}

	private Indoor indoorDeLaPlanta(EmpleadoIndoor empleado, int plantaId) {
		for (Indoor indoor : empleado.getSectoresACargo()) {
			if (indoor.buscarPlanta(plantaId) != null) {
				return indoor;
			}
		}
		throw new RecursoNoEncontradoException("La planta " + plantaId + " no está en tus indoors");
	}
}
