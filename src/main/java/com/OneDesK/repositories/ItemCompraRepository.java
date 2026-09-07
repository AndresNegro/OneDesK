package com.OneDesK.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.OneDesK.modelo.ItemCompra;

@Repository
public interface ItemCompraRepository extends JpaRepository<ItemCompra,Integer>{

}
