package com.OneDesK.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.OneDesK.modelo.Compra;
import com.OneDesK.modelo.EstadoCompra;

@Repository
public interface CompraRepository extends JpaRepository<Compra,Integer> {

	// registros de pago: las compras que tienen fecha de pago, de la mas reciente a la mas vieja
	List<Compra> findByFechaPagoIsNotNullOrderByFechaPagoDescIdDesc();

	// las compras de un usuario, de la mas reciente a la mas vieja
	List<Compra> findByUsuarioIdOrderByFechaCompraDescIdDesc(int usuarioId);

	// las compras aprobadas sin pagar de todos los usuarios, de la mas vieja a la mas reciente
	List<Compra> findByPagadoFalseAndEstadoOrderByFechaCompraAscIdAsc(EstadoCompra estado);

	// las compras en un estado, de la mas vieja a la mas reciente
	List<Compra> findByEstadoOrderByFechaCompraAscIdAsc(EstadoCompra estado);
}
