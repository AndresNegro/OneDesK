package com.OneDesK.services;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.OneDesK.web.Mensajes;

/**
 * Manda los avisos por mail. El texto sale de messages.properties, como todo lo que se lee.
 *
 * Con onedesk.mail.activo en false no se manda nada y el aviso queda escrito en el log: asi la
 * aplicacion anda en cualquier maquina sin tener que configurar un servidor de correo.
 */
@Service
public class CorreoServiceImpl implements CorreoService {

	private static final Logger log = LoggerFactory.getLogger(CorreoServiceImpl.class);

	/**
	 * Los mails salen siempre en espanol: quien los recibe no esta mirando la pagina, asi que no hay
	 * un idioma elegido para ese pedido.
	 */
	private static final Locale IDIOMA = Locale.forLanguageTag("es");

	// puede no estar: el bean lo arma Spring solo si hay un servidor configurado (spring.mail.host)
	@Autowired(required = false)
	private JavaMailSender correo;
	@Autowired
	private Mensajes mensajes;

	@Value("${onedesk.mail.activo:false}")
	private boolean activo;
	@Value("${onedesk.mail.remitente:no-responder@onedesk.com}")
	private String remitente;

	@Override
	public void avisarQueLaCuentaFueAprobada(String nombre, String email, int topeCredito) {
		enviar(email, mensajes.de("mail.aprobada.asunto", IDIOMA),
				mensajes.de("mail.aprobada.cuerpo", IDIOMA, nombre, topeCredito));
	}

	@Override
	public void avisarQueLaSolicitudFueRechazada(String nombre, String email) {
		enviar(email, mensajes.de("mail.rechazada.asunto", IDIOMA),
				mensajes.de("mail.rechazada.cuerpo", IDIOMA, nombre));
	}

	/**
	 * Un mail que no sale no deshace lo que ya se hizo: la cuenta quedo aprobada igual, asi que el
	 * problema se anota en el log y la operacion sigue.
	 */
	private void enviar(String destinatario, String asunto, String cuerpo) {
		if (!activo) {
			log.info("Correo apagado: no se mando a {} el aviso \"{}\"", destinatario, asunto);
			return;
		}
		if (correo == null) {
			log.warn("El correo esta activo pero falta configurar spring.mail.host: no se mando a {}"
					+ " el aviso \"{}\"", destinatario, asunto);
			return;
		}
		SimpleMailMessage mail = new SimpleMailMessage();
		mail.setFrom(remitente);
		mail.setTo(destinatario);
		mail.setSubject(asunto);
		mail.setText(cuerpo);
		try {
			correo.send(mail);
			log.info("Se le mando a {} el aviso \"{}\"", destinatario, asunto);
		} catch (MailException noSePudoMandar) {
			log.warn("No se pudo mandar a {} el aviso \"{}\"", destinatario, asunto, noSePudoMandar);
		}
	}
}
