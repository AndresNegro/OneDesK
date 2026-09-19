package com.OneDesK.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.OneDesK.modelo.LimitesDeCompra;

@Repository
public interface LimitesDeCompraRepository extends JpaRepository<LimitesDeCompra, Integer> {

}
