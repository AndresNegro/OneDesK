package com.OneDesK.services;

import java.util.List;

import com.OneDesK.dtos.DTO;

/** El service que sabe entregar lo suyo listo para la API, ya convertido en DTOs. */
public interface ExponibleService {

	List<DTO> exponer();
}
