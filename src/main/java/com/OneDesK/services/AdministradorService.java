package com.OneDesK.services;

import java.util.List;

import com.OneDesK.evento.Evento;
import com.OneDesK.modelo.Administrador;
import com.OneDesK.modelo.Compra;
import com.OneDesK.modelo.EmpleadoIndoor;
import com.OneDesK.modelo.Indoor;
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

	public Indoor crearIndoor(int adminId);

	public EmpleadoIndoor registrarEmpleado(int adminId, String nombre, String apellido, String email,
			String contrasenia, int salarioMensual);

	public void asignarEmpleado(int adminId, int empleadoId, int indoorId);

	public void desasignarEmpleado(int adminId, int empleadoId, int indoorId);

	public EmpleadoIndoor cambiarSalario(int adminId, int empleadoId, int salarioMensual);

	// --- productos ---

	public Producto crearProducto(int adminId, String genetica, int precio);

	public Producto cambiarPrecio(int adminId, int productoId, int precio);

	/** Correccion de inventario: deja el stock en el valor contado. */
	public Producto fijarStock(int adminId, int productoId, int stock);

	/** Suma (positivo) o resta (negativo) al stock actual. */
	public Producto ajustarStock(int adminId, int productoId, int cantidad);

	// --- usuarios y compras ---

	/** Registra un usuario con su tope de credito ya asignado. */
	public Usuario registrarUsuario(int adminId, String nombre, String apellido, String email, String contrasenia,
			int topeCredito);

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
