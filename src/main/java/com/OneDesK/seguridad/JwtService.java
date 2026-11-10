package com.OneDesK.seguridad;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Arma y revisa los tokens con los que se usa la API. El token dice de quien es (el email) y hasta
 * cuando vale, y va firmado: si alguien le cambia una letra, la firma deja de dar y no se acepta.
 *
 * La clave de la firma sale de db-local.properties, que no esta en el repositorio. Si no hay
 * ninguna se genera una al arrancar: la aplicacion anda igual, pero los tokens dejan de valer en
 * cada reinicio, que es justo lo que se quiere en una maquina de desarrollo y no en produccion.
 */
@Service
public class JwtService {

	private final SecretKey clave;
	private final long minutosQueDura;

	public JwtService(@Value("${onedesk.jwt.secreto:}") String secreto,
			@Value("${onedesk.jwt.minutos:60}") long minutosQueDura) {
		this.clave = secreto.isBlank() ? Jwts.SIG.HS256.key().build()
				: Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto));
		this.minutosQueDura = minutosQueDura;
	}

	public String generar(DetallesDePersona quienEntro) {
		Date ahora = new Date();
		return Jwts.builder()
				.subject(quienEntro.getUsername())
				.claim("rol", quienEntro.getRol().name())
				.issuedAt(ahora)
				.expiration(new Date(ahora.getTime() + minutosQueDura * 60 * 1000))
				.signWith(clave)
				.compact();
	}

	public long getMinutosQueDura() {
		return minutosQueDura;
	}

	/** El email de quien mando el token, o null si el token esta vencido, cambiado o mal firmado. */
	public String emailDe(String token) {
		try {
			Claims contenido = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
			return contenido.getSubject();
		} catch (JwtException | IllegalArgumentException token_invalido) {
			return null;
		}
	}
}
