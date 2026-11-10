package com.OneDesK.seguridad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.OneDesK.enums.Permiso;
import com.OneDesK.enums.Rol;
import com.OneDesK.modelo.Persona;

/**
 * La persona de OneDesK vista como la mira Spring Security. De cada rol salen dos cosas: el rol en
 * si, con el prefijo ROLE_ que Spring espera, y cada uno de sus permisos, que son con los que se
 * autorizan las paginas.
 */
public class DetallesDePersona implements UserDetails {

	private static final long serialVersionUID = 1L;

	private final int id;
	private final String email;
	private final String contrasenia;
	private final String nombreCompleto;
	private final Rol rol;
	private final boolean habilitada;
	private final List<GrantedAuthority> permisos;

	public DetallesDePersona(Persona persona) {
		this.id = persona.getId();
		this.email = persona.getEmail();
		this.contrasenia = persona.getContrasenia();
		this.nombreCompleto = persona.getNombreCompleto();
		this.rol = persona.getRol();
		this.habilitada = persona.estaHabilitada();
		this.permisos = autoridadesDe(persona.getRoles());
	}

	private static List<GrantedAuthority> autoridadesDe(List<Rol> roles) {
		List<GrantedAuthority> autoridades = new ArrayList<>();
		for (Rol rol : roles) {
			autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
			for (Permiso permiso : rol.getPermisos()) {
				autoridades.add(new SimpleGrantedAuthority(permiso.name()));
			}
		}
		return autoridades;
	}

	public int getId() {
		return id;
	}

	public String getNombreCompleto() {
		return nombreCompleto;
	}

	public Rol getRol() {
		return rol;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return permisos;
	}

	@Override
	public String getPassword() {
		return contrasenia;
	}

	// en OneDesK se entra con el email, no con un nombre de usuario aparte
	@Override
	public String getUsername() {
		return email;
	}

	/** Un cliente que todavia no fue aprobado no puede entrar: Spring lo rechaza con DisabledException. */
	@Override
	public boolean isEnabled() {
		return habilitada;
	}
}
