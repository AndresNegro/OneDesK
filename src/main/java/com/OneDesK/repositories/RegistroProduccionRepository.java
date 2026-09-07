package com.OneDesK.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.OneDesK.modelo.RegistroProduccion;

@Repository
public interface RegistroProduccionRepository extends JpaRepository<RegistroProduccion,Integer>{

}
