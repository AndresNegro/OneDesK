package com.OneDesK.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.OneDesK.modelo.Deuda;

@Repository
public interface DeudaRepository extends JpaRepository<Deuda,Integer>{

}
