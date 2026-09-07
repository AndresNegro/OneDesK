package com.OneDesK.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.OneDesK.modelo.EmpleadoIndoor;
@Repository
public interface EmpleadoIndoorRepository extends JpaRepository<EmpleadoIndoor,Integer>{

}
