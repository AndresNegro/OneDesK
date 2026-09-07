package com.OneDesK.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.OneDesK.modelo.Planta;

@Repository
public interface PlantaRepository extends JpaRepository<Planta,Integer>{

}
