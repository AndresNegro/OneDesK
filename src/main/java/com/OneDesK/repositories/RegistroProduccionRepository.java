package com.OneDesK.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.OneDesK.modelo.RegistroProduccion;

@Repository
public interface RegistroProduccionRepository extends JpaRepository<RegistroProduccion,Integer>{

	// de la cosecha mas reciente a la mas vieja
	List<RegistroProduccion> findAllByOrderByFechaRegistroDescIdDesc();
}
