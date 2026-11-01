package com.OneDesK.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.OneDesK.dtos.IndoorDTO;
import com.OneDesK.dtos.ProductoDTO;
import com.OneDesK.dtos.UsuarioAdminDTO;
import com.OneDesK.dtos.UsuarioDTO;
import com.OneDesK.modelo.Indoor;
import com.OneDesK.modelo.Producto;
import com.OneDesK.modelo.Usuario;

/**
 * El mapeador que arma los DTO copiando los campos que se llaman igual en la entidad y en el DTO.
 *
 * Con la estrategia estricta solo copia los que coinciden exacto: nada de adivinar por parecido,
 * que es como un mapeador te llena un campo con el valor de otro sin que te enteres.
 *
 * Lo que no es una copia (la deuda, que se recalcula; las plantas en cultivo, que son una cuenta)
 * se completa despues del mapeo, y queda escrito aca en un solo lugar.
 */
@Configuration
public class ConfiguracionDeMapeo {

	@Bean
	public ModelMapper modelMapper() {
		ModelMapper mapeador = new ModelMapper();
		mapeador.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

		mapearProducto(mapeador);
		mapearUsuario(mapeador);
		mapearUsuarioParaAdmin(mapeador);
		mapearIndoor(mapeador);

		return mapeador;
	}

	// genetica, stock y precio se llaman igual de los dos lados: no hay nada que configurar
	private void mapearProducto(ModelMapper mapeador) {
		mapeador.typeMap(Producto.class, ProductoDTO.class);
	}

	private void mapearUsuario(ModelMapper mapeador) {
		mapeador.typeMap(Usuario.class, UsuarioDTO.class)
				.addMappings(mapeo -> mapeo.skip(UsuarioDTO::setDeuda))
				.setPostConverter(contexto -> {
					contexto.getDestination().setDeuda(contexto.getSource().montoDeDeuda());
					return contexto.getDestination();
				});
	}

	private void mapearUsuarioParaAdmin(ModelMapper mapeador) {
		mapeador.typeMap(Usuario.class, UsuarioAdminDTO.class)
				.addMappings(mapeo -> {
					mapeo.skip(UsuarioAdminDTO::setDeuda);
					mapeo.skip(UsuarioAdminDTO::setDeudaComprometida);
					mapeo.skip(UsuarioAdminDTO::setCompras);
				})
				.setPostConverter(contexto -> {
					Usuario usuario = contexto.getSource();
					UsuarioAdminDTO dto = contexto.getDestination();
					dto.setDeuda(usuario.montoDeDeuda());
					dto.setDeudaComprometida(usuario.deudaComprometida());
					dto.setCompras(usuario.getCompras().size());
					return dto;
				});
	}

	private void mapearIndoor(ModelMapper mapeador) {
		mapeador.typeMap(Indoor.class, IndoorDTO.class)
				.addMappings(mapeo -> {
					mapeo.skip(IndoorDTO::setPlantasEnCultivo);
					mapeo.skip(IndoorDTO::setEventosPendientes);
				})
				.setPostConverter(contexto -> {
					Indoor indoor = contexto.getSource();
					IndoorDTO dto = contexto.getDestination();
					dto.setPlantasEnCultivo(indoor.plantasEnCultivo());
					dto.setEventosPendientes(indoor.getEventosPendientes().size());
					return dto;
				});
	}
}
