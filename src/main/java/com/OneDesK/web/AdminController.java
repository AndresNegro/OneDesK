package com.OneDesK.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.OneDesK.evento.Evento;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Planta;
import com.OneDesK.modelo.Usuario;
import com.OneDesK.services.AdministradorService;

import jakarta.servlet.http.HttpSession;

/**
 * La pagina de administracion. Cada accion vuelve a la pestana desde la que se hizo (el #seccion
 * de la direccion), asi no hay que buscarla de nuevo.
 */
@Controller
public class AdminController {

	@Autowired
	private AdministradorService admin;

	@GetMapping("/admin")
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
			for (Planta planta : indoor.getPlantas()) {
				if (!planta.isCosechada()) {
					plantasEnCultivo++;
				}
			}
		}
		int sinIndoor = 0;
		for (EmpleadoIndoor empleado : empleados) {
			if (empleado.getSectoresACargo().isEmpty()) {
				sinIndoor++;
			}
		}
		int deudaTotal = 0;
		for (Usuario deudor : deudores) {
			deudaTotal += deudor.getDeuda().getMonto();
		}
		modelo.addAttribute("plantasEnCultivo", plantasEnCultivo);
		modelo.addAttribute("empleadosSinIndoor", sinIndoor);
		modelo.addAttribute("deudaTotal", deudaTotal);
		return "admin";
	}

	// --- indoors y empleados ---

	@PostMapping("/admin/indoors")
	public String crearIndoor(HttpSession sesion, RedirectAttributes flash) {
		Indoor indoor = admin.crearIndoor(Sesion.personaId(sesion));
		flash.addFlashAttribute("exito", "Creaste el Indoor " + indoor.getId());
		return "redirect:/admin#indoors";
	}

	@PostMapping("/admin/plantar")
	public String plantar(@RequestParam int indoorId, @RequestParam String genetica,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaGerminado,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaPlantado,
			@RequestParam int tiempoRegado, @RequestParam int tiempoLuz, @RequestParam int tiempoVentilacion,
			HttpSession sesion, RedirectAttributes flash) {
		Planta planta = new Planta(genetica, fechaPlantado, fechaGerminado, tiempoRegado, tiempoLuz,
				tiempoVentilacion);
		admin.plantar(Sesion.personaId(sesion), indoorId, planta);
		flash.addFlashAttribute("exito", "Plantaste " + planta.getGenetica() + " en el Indoor " + indoorId);
		return "redirect:/admin#indoors";
	}

	@PostMapping("/admin/empleados")
	public String registrarEmpleado(@RequestParam String nombre, @RequestParam String apellido,
			@RequestParam String email, @RequestParam String contrasenia, @RequestParam int salario,
			HttpSession sesion, RedirectAttributes flash) {
		admin.registrarEmpleado(Sesion.personaId(sesion), nombre, apellido, email, contrasenia, salario);
		flash.addFlashAttribute("exito", "Diste de alta a " + nombre.trim() + " " + apellido.trim());
		return "redirect:/admin#indoors";
	}

	@PostMapping("/admin/empleados/asignar")
	public String asignar(@RequestParam int empleadoId, @RequestParam int indoorId, HttpSession sesion,
			RedirectAttributes flash) {
		admin.asignarEmpleado(Sesion.personaId(sesion), empleadoId, indoorId);
		flash.addFlashAttribute("exito", "Asignación guardada");
		return "redirect:/admin#indoors";
	}

	@PostMapping("/admin/empleados/desasignar")
	public String desasignar(@RequestParam int empleadoId, @RequestParam int indoorId, HttpSession sesion,
			RedirectAttributes flash) {
		admin.desasignarEmpleado(Sesion.personaId(sesion), empleadoId, indoorId);
		flash.addFlashAttribute("exito", "Desasignaste el Indoor " + indoorId);
		return "redirect:/admin#indoors";
	}

	@PostMapping("/admin/empleados/salario")
	public String salario(@RequestParam int empleadoId, @RequestParam int salario, HttpSession sesion,
			RedirectAttributes flash) {
		admin.cambiarSalario(Sesion.personaId(sesion), empleadoId, salario);
		flash.addFlashAttribute("exito", "Salario actualizado");
		return "redirect:/admin#indoors";
	}

	// --- productos ---

	@PostMapping("/admin/productos")
	public String crearProducto(@RequestParam String genetica, @RequestParam int precio, HttpSession sesion,
			RedirectAttributes flash) {
		admin.crearProducto(Sesion.personaId(sesion), genetica, precio);
		flash.addFlashAttribute("exito", "Diste de alta " + genetica.trim());
		return "redirect:/admin#productos";
	}

	@PostMapping("/admin/productos/precio")
	public String precio(@RequestParam int productoId, @RequestParam int precio, HttpSession sesion,
			RedirectAttributes flash) {
		admin.cambiarPrecio(Sesion.personaId(sesion), productoId, precio);
		flash.addFlashAttribute("exito", "Precio actualizado");
		return "redirect:/admin#productos";
	}

	@PostMapping("/admin/productos/fijar")
	public String fijarStock(@RequestParam int productoId, @RequestParam int stock, HttpSession sesion,
			RedirectAttributes flash) {
		admin.fijarStock(Sesion.personaId(sesion), productoId, stock);
		flash.addFlashAttribute("exito", "Stock fijado en " + stock);
		return "redirect:/admin#productos";
	}

	@PostMapping("/admin/productos/ajustar")
	public String ajustarStock(@RequestParam int productoId, @RequestParam int cantidad, HttpSession sesion,
			RedirectAttributes flash) {
		admin.ajustarStock(Sesion.personaId(sesion), productoId, cantidad);
		flash.addFlashAttribute("exito", "Stock ajustado");
		return "redirect:/admin#productos";
	}

	// --- usuarios y compras ---

	@PostMapping("/admin/solicitudes/aprobar")
	public String aprobar(@RequestParam int usuarioId, @RequestParam int tope, HttpSession sesion,
			RedirectAttributes flash) {
		Usuario usuario = admin.aprobarUsuario(Sesion.personaId(sesion), usuarioId, tope);
		flash.addFlashAttribute("exito", "Aprobaste a " + usuario.getNombre() + " " + usuario.getApellido());
		return "redirect:/admin#usuarios";
	}

	@PostMapping("/admin/solicitudes/rechazar")
	public String rechazar(@RequestParam int usuarioId, HttpSession sesion, RedirectAttributes flash) {
		admin.rechazarUsuario(Sesion.personaId(sesion), usuarioId);
		flash.addFlashAttribute("exito", "Rechazaste la solicitud");
		return "redirect:/admin#usuarios";
	}

	@PostMapping("/admin/usuarios/tope")
	public String tope(@RequestParam int usuarioId, @RequestParam int tope, HttpSession sesion,
			RedirectAttributes flash) {
		admin.asignarTope(Sesion.personaId(sesion), usuarioId, tope);
		flash.addFlashAttribute("exito", "Tope actualizado");
		return "redirect:/admin#usuarios";
	}

	@PostMapping("/admin/compras/anular")
	public String anular(@RequestParam int compraId, HttpSession sesion, RedirectAttributes flash) {
		admin.anularCompra(Sesion.personaId(sesion), compraId);
		flash.addFlashAttribute("exito", "Anulaste la compra #" + compraId);
		return "redirect:/admin#compras";
	}

	@PostMapping("/admin/compras/aprobar")
	public String aprobarCompra(@RequestParam int compraId, HttpSession sesion, RedirectAttributes flash) {
		admin.aprobarCompra(Sesion.personaId(sesion), compraId);
		flash.addFlashAttribute("exito", "Aprobaste la compra #" + compraId);
		return "redirect:/admin#compras";
	}

	@PostMapping("/admin/compras/rechazar")
	public String rechazarCompra(@RequestParam int compraId, HttpSession sesion, RedirectAttributes flash) {
		admin.rechazarCompra(Sesion.personaId(sesion), compraId);
		flash.addFlashAttribute("exito", "Rechazaste la compra #" + compraId + " y el stock volvió al catálogo");
		return "redirect:/admin#compras";
	}

	@PostMapping("/admin/limites")
	public String limites(@RequestParam int minimo, @RequestParam int maximo, HttpSession sesion,
			RedirectAttributes flash) {
		admin.cambiarLimites(Sesion.personaId(sesion), minimo, maximo);
		flash.addFlashAttribute("exito", "Ahora cada compra va de " + minimo + " g a " + maximo + " g");
		return "redirect:/admin#compras";
	}
}
