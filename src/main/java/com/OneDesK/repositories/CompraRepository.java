package com.OneDesK.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.OneDesK.modelo.Compra;

@Repository
public interface CompraRepository extends JpaRepository<Compra,Integer> {

	// registros de pago: las compras que tienen fecha de pago, de la mas reciente a la mas vieja
	List<Compra> findByFechaPagoIsNotNullOrderByFechaPagoDescIdDesc();
}
