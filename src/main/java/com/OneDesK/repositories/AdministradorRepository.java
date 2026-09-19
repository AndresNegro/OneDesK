package com.OneDesK.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.OneDesK.modelo.Administrador;

@Repository
public interface AdministradorRepository extends JpaRepository<Administrador,Integer> {

}
