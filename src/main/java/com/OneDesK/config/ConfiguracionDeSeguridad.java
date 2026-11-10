package com.OneDesK.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpStatus;

import com.OneDesK.enums.Permiso;
import com.OneDesK.seguridad.AccesoDenegado;
import com.OneDesK.seguridad.FalloAlIngresar;
import com.OneDesK.seguridad.ExitoAlIngresar;
import com.OneDesK.seguridad.FiltroJwt;
import com.OneDesK.web.AccesoController;
import com.OneDesK.web.AdminController;
import com.OneDesK.web.CatalogoController;
import com.OneDesK.web.EmpleadoController;
import com.OneDesK.web.MisComprasController;
import com.OneDesK.web.rest.UsuarioAPI;
import com.OneDesK.web.rest.AutenticacionAPI;

/**
 * Quien puede entrar a cada cosa. Hay dos reglas separadas porque son dos mundos distintos: la API
 * no tiene sesion ni formulario (cada pedido trae su token) y la web si.
 *
 * Las zonas no se autorizan por rol sino por permiso: la pagina pide lo que hay que saber hacer
 * para verla, y que rol tiene ese permiso lo decide el enum Rol.
 */
@Configuration
@EnableWebSecurity
public class ConfiguracionDeSeguridad {

	@Autowired
	private ExitoAlIngresar exitoAlIngresar;
	@Autowired
	private FalloAlIngresar falloAlIngresar;
	@Autowired
	private AccesoDenegado accesoDenegado;
	@Autowired
	private FiltroJwt filtroJwt;

	/** Lo usa el login de la API para revisar email y contrasenia antes de dar el token. */
	@Bean
	public AuthenticationManager autenticador(AuthenticationConfiguration configuracion) throws Exception {
		return configuracion.getAuthenticationManager();
	}

	// la API va primero: sin sesion, sin formulario de login y sin CSRF, porque no hay navegador
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public SecurityFilterChain api(HttpSecurity http) throws Exception {
		http.securityMatcher("/api/**")
				.csrf(csrf -> csrf.disable())
				.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(rutas -> rutas
						.requestMatchers(AutenticacionAPI.LOGIN_URL, UsuarioAPI.GUARDAR_USUARIO_URL).permitAll()
						.requestMatchers("/api/admin/**").hasAuthority(Permiso.ADMINISTRAR_NEGOCIO.name())
						.anyRequest().authenticated())
				// sin token valido la API contesta 401 y no manda a ninguna pagina: no hay a donde ir
				.exceptionHandling(errores -> errores
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public SecurityFilterChain web(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(rutas -> rutas
						.requestMatchers(AccesoController.LOGIN_URL, AccesoController.REGISTRO_URL).permitAll()
						.requestMatchers("/css/**", "/img/**", "/favicon.ico", "/error").permitAll()
						.requestMatchers(bajoLaRuta(AdminController.ADMIN_URL))
						.hasAuthority(Permiso.ADMINISTRAR_NEGOCIO.name())
						.requestMatchers(bajoLaRuta(EmpleadoController.EMPLEADO_URL))
						.hasAuthority(Permiso.CUIDAR_PLANTAS.name())
						.requestMatchers(bajoLaRuta(CatalogoController.CATALOGO_URL),
								bajoLaRuta(CatalogoController.CARRITO_URL),
								bajoLaRuta(MisComprasController.MIS_COMPRAS_URL))
						.hasAuthority(Permiso.COMPRAR.name())
						.anyRequest().authenticated())
				.formLogin(ingreso -> ingreso
						.loginPage(AccesoController.LOGIN_URL)
						.loginProcessingUrl(AccesoController.INGRESAR_URL)
						.usernameParameter("email")
						.passwordParameter("contrasenia")
						.successHandler(exitoAlIngresar)
						.failureHandler(falloAlIngresar)
						.permitAll())
				.logout(salida -> salida
						.logoutUrl(AccesoController.SALIR_URL)
						.logoutSuccessUrl(AccesoController.LOGIN_URL)
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID"))
				// al entrar, la sesion cambia de id: un id que alguien hubiera fijado antes deja de servir
				.sessionManagement(sesion -> sesion.sessionFixation().changeSessionId())
				.exceptionHandling(errores -> errores.accessDeniedHandler(accesoDenegado));
		return http.build();
	}

	// la pagina y todo lo que cuelga de ella: /admin y /admin/lo-que-sea
	private String bajoLaRuta(String url) {
		return url + "/**";
	}
}
