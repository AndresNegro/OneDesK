package com.OneDesK.services;

import java.util.List;

import com.OneDesK.evento.Evento;
import com.OneDesK.modelo.Administrador;
import com.OneDesK.modelo.Compra;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.LimitesDeCompra;
import com.OneDesK.modelo.Planta;
import com.OneDesK.modelo.Producto;
import com.OneDesK.modelo.RegistroProduccion;
import com.OneDesK.modelo.Usuario;

/**
 * Las acciones de administracion del negocio. Todas reciben el id de quien las pide y fallan con
 * RecursoNoEncontradoException si no es un administrador: un usuario o un empleado no pueden usarlas.
 */
public interface AdministradorService {

	/** Da de alta un administrador. El email no puede estar usado por otra persona. */
	public Administrador registrar(String nombre, String apellido, String email, String contrasenia);

	// --- indoors y empleados ---

	public Indoor crearIndoor(int adminId, String nombre, int capacidad);

	public Indoor editarIndoor(int adminId, int indoorId, String nombre, int capacidad);

	public EmpleadoIndoor registrarEmpleado(int adminId, String nombre, String apellido, String email,
			String contrasenia, int salarioMensual);

	public void asignarEmpleado(int adminId, int empleadoId, int indoorId);

	public void desasignarEmpleado(int adminId, int empleadoId, int indoorId);

	public EmpleadoIndoor cambiarSalario(int adminId, int empleadoId, int salarioMensual);

	/** El administrador puede plantar en cualquier indoor. */
	public Planta plantar(int adminId, int indoorId, Planta planta);

	// --- listados para el panel ---

	public List<Indoor> indoors(int adminId);

	public List<EmpleadoIndoor> empleados(int adminId);

	/** Todos los productos, tambien los que no tienen stock. */
	public List<Producto> productos(int adminId);

	/** Los usuarios con la cuenta aprobada: las solicitudes pendientes van aparte. */
	public List<Usuario> usuarios(int adminId);

	/** Las compras aprobadas que todavia no se pagaron, de todos los usuarios. */
	public List<Compra> comprasImpagas(int adminId);

	// --- compras por aprobar y limites ---

	/** Las compras que esperan respuesta, de la mas vieja a la mas nueva. */
	public List<Compra> comprasPendientes(int adminId);

	public Compra aprobarCompra(int adminId, int compraId);

	public Compra rechazarCompra(int adminId, int compraId);

	public LimitesDeCompra limitesDeCompra(int adminId);

	/** Cambia el minimo y el maximo de gramos por compra: rige para las compras que se pidan desde ahora. */
	public LimitesDeCompra cambiarLimites(int adminId, int minimoGramos, int maximoGramos);

	// --- productos ---

	public Producto crearProducto(int adminId, String genetica, int precio);

	public Producto cambiarPrecio(int adminId, int productoId, int precio);

	/** Correccion de inventario: deja el stock en el valor contado. */
	public Producto fijarStock(int adminId, int productoId, int stock);

	/** Suma (positivo) o resta (negativo) al stock actual. */
	public Producto ajustarStock(int adminId, int productoId, int cantidad);

	// --- usuarios y compras ---

	/** Las solicitudes de registro que esperan respuesta, de la mas vieja a la mas nueva. */
	public List<Usuario> solicitudesPendientes(int adminId);

	/** Acepta la solicitud y le asigna el tope de credito: desde ahi el usuario puede ingresar y comprar. */
	public Usuario aprobarUsuario(int adminId, int usuarioId, int topeCredito);

	/** Rechaza la solicitud y la borra: esa persona puede volver a registrarse con el mismo email. */
	public void rechazarUsuario(int adminId, int usuarioId);

	public Usuario asignarTope(int adminId, int usuarioId, int topeCredito);

	/** Anula la compra impaga de cualquier usuario y devuelve el stock. Una compra pagada no se anula. */
	public void anularCompra(int adminId, int compraId);

	// --- reportes ---

	/** Todas las cosechas registradas, de la mas reciente a la mas vieja. */
	public List<RegistroProduccion> registrosDeProduccion(int adminId);

	/** Las compras pagadas con su fecha de pago, de la mas reciente a la mas vieja. */
	public List<Compra> registrosDePago(int adminId);

	/** Los usuarios que deben plata, del que mas debe al que menos. */
	public List<Usuario> deudores(int adminId);

	/** Los eventos sin atender de todos los indoors. */
	public List<Evento> eventosPendientes(int adminId);
}
