package com.OneDesK.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.OneDesK.evento.Evento;
import com.OneDesK.excepciones.EmailDuplicadoException;
import com.OneDesK.excepciones.RecursoNoEncontradoException;
import com.OneDesK.modelo.Administrador;
import com.OneDesK.modelo.Compra;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Producto;
import com.OneDesK.modelo.RegistroProduccion;
import com.OneDesK.modelo.Usuario;
import com.OneDesK.repositories.AdministradorRepository;
import com.OneDesK.repositories.CompraRepository;
import com.OneDesK.repositories.IndoorRepository;
import com.OneDesK.repositories.PersonaRepository;
import com.OneDesK.repositories.RegistroProduccionRepository;
import com.OneDesK.repositories.UsuarioRepository;

// El administrador no repite reglas de negocio: verifica que quien pide sea administrador
// y delega en los services que ya las tienen, asi cada regla vive en un solo lugar.
@Service
public class AdministradorServiceImpl implements AdministradorService {

	private static final int SIN_DEUDA = 0;

	@Autowired
	private AdministradorRepository repositorio;
	@Autowired
	private PersonaRepository personaRepository;
	@Autowired
	private IndoorRepository indoorRepository;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private CompraRepository compraRepository;
	@Autowired
	private RegistroProduccionRepository registroProduccionRepository;

	@Autowired
	private IndoorService indoorService;
	@Autowired
	private EmpleadoIndoorService empleadoService;
	@Autowired
	private ProductoService productoService;
	@Autowired
	private UsuarioService usuarioService;
	@Autowired
	private CompraService compraService;

	@Override
	@Transactional
	public Administrador registrar(String nombre, String apellido, String email, String contrasenia) {
		Administrador administrador = new Administrador(nombre, apellido, email, contrasenia);

		if (personaRepository.existsByEmail(administrador.getEmail())) {
			throw new EmailDuplicadoException(
					"Ya existe una persona registrada con el email " + administrador.getEmail());
		}
		return repositorio.save(administrador);
	}

	// --- indoors y empleados ---

	@Override
	@Transactional
	public Indoor crearIndoor(int adminId) {
		verificarAdministrador(adminId);
		return indoorService.crearIndoor();
	}

	@Override
	@Transactional
	public EmpleadoIndoor registrarEmpleado(int adminId, String nombre, String apellido, String email,
			String contrasenia, int salarioMensual) {
		verificarAdministrador(adminId);
		return empleadoService.registrar(nombre, apellido, email, contrasenia, salarioMensual);
	}

	@Override
	@Transactional
	public void asignarEmpleado(int adminId, int empleadoId, int indoorId) {
		verificarAdministrador(adminId);
		empleadoService.asignarIndoor(empleadoId, indoorId);
	}

	@Override
	@Transactional
	public void desasignarEmpleado(int adminId, int empleadoId, int indoorId) {
		verificarAdministrador(adminId);
		empleadoService.desasignarIndoor(empleadoId, indoorId);
	}

	@Override
	@Transactional
	public EmpleadoIndoor cambiarSalario(int adminId, int empleadoId, int salarioMensual) {
		verificarAdministrador(adminId);
		return empleadoService.cambiarSalario(empleadoId, salarioMensual);
	}

	// --- productos ---

	@Override
	@Transactional
	public Producto crearProducto(int adminId, String genetica, int precio) {
		verificarAdministrador(adminId);
		return productoService.crearProducto(genetica, precio);
	}

	@Override
	@Transactional
	public Producto cambiarPrecio(int adminId, int productoId, int precio) {
		verificarAdministrador(adminId);
		return productoService.cambiarPrecio(productoId, precio);
	}

	@Override
	@Transactional
	public Producto fijarStock(int adminId, int productoId, int stock) {
		verificarAdministrador(adminId);
		return productoService.fijarStock(productoId, stock);
	}

	@Override
	@Transactional
	public Producto ajustarStock(int adminId, int productoId, int cantidad) {
		verificarAdministrador(adminId);
		return productoService.ajustarStock(productoId, cantidad);
	}

	// --- usuarios y compras ---

	@Override
	@Transactional
	public Usuario registrarUsuario(int adminId, String nombre, String apellido, String email, String contrasenia,
			int topeCredito) {
		verificarAdministrador(adminId);
		// el tope se valida antes de registrar, para no guardar un usuario y despues rechazar su tope
		if (topeCredito < 0) {
			throw new IllegalArgumentException("El tope de credito no puede ser negativo");
		}
		Usuario usuario = usuarioService.registrar(nombre, apellido, email, contrasenia);
		usuario.setTopeCredito(topeCredito);
		return usuario;
	}

	@Override
	@Transactional
	public Usuario asignarTope(int adminId, int usuarioId, int topeCredito) {
		verificarAdministrador(adminId);
		return usuarioService.asignarTopeCredito(usuarioId, topeCredito);
	}

	@Override
	@Transactional
	public void anularCompra(int adminId, int compraId) {
		verificarAdministrador(adminId);
		compraService.anularCompra(compraId);
	}

	// --- reportes ---

	@Override
	@Transactional
	public List<RegistroProduccion> registrosDeProduccion(int adminId) {
		verificarAdministrador(adminId);
		return registroProduccionRepository.findAllByOrderByFechaRegistroDescIdDesc();
	}

	@Override
	@Transactional
	public List<Compra> registrosDePago(int adminId) {
		verificarAdministrador(adminId);
		return compraRepository.findByFechaPagoIsNotNullOrderByFechaPagoDescIdDesc();
	}

	@Override
	@Transactional
	public List<Usuario> deudores(int adminId) {
		verificarAdministrador(adminId);
		return usuarioRepository.findByDeudaMontoGreaterThanOrderByDeudaMontoDesc(SIN_DEUDA);
	}

	@Override
	@Transactional
	public List<Evento> eventosPendientes(int adminId) {
		verificarAdministrador(adminId);
		List<Evento> pendientes = new ArrayList<>();
		for (Indoor indoor : indoorRepository.findAll()) {
			pendientes.addAll(indoor.getEventosPendientes());
		}
		return pendientes;
	}

	// un id de usuario o de empleado no esta en la tabla Administrador, asi que tambien se rechaza
	private void verificarAdministrador(int adminId) {
		if (!repositorio.existsById(adminId)) {
			throw new RecursoNoEncontradoException("No existe el administrador " + adminId);
		}
	}
}
