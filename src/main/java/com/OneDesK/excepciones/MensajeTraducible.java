package com.OneDesK.excepciones;

/** Un error que sabe con que clave de messages.properties se traduce y con que datos. */
public interface MensajeTraducible {

	String getClave();

	Object[] getArgumentos();
}
