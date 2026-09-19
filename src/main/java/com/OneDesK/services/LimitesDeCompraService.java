package com.OneDesK.services;

import com.OneDesK.modelo.LimitesDeCompra;

public interface LimitesDeCompraService {

	/** Los limites vigentes. Si todavia no hay ninguno guardado, se crean con 5 g y 40 g. */
	public LimitesDeCompra obtener();

	public LimitesDeCompra cambiar(int minimoGramos, int maximoGramos);
}
