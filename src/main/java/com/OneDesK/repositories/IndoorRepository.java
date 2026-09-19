package com.OneDesK.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.OneDesK.modelo.Indoor;

@Repository
public interface IndoorRepository extends JpaRepository<Indoor,Integer>{

	boolean existsByNombreIgnoreCase(String nombre);

	// al editar, el nombre puede repetirse solo con el mismo indoor
	boolean existsByNombreIgnoreCaseAndIdNot(String nombre, int id);
}
