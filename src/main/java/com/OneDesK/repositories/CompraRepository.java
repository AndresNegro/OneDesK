package com.OneDesK.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.OneDesK.modelo.Compra;

@Repository
public interface CompraRepository extends JpaRepository<Compra,Integer> {

}
