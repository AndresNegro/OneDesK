package com.OneDesK.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.OneDesK.modelo.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario,Integer>{

	// los usuarios cuya deuda supera ese monto, del que mas debe al que menos
	List<Usuario> findByDeudaMontoGreaterThanOrderByDeudaMontoDesc(int monto);
}
