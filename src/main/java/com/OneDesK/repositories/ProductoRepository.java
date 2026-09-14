package com.OneDesK.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.OneDesK.modelo.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto,Integer> {

	Optional<Producto> findByGeneticaIgnoreCase(String genetica);

	boolean existsByGeneticaIgnoreCase(String genetica);
}
