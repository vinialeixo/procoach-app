package com.fitness.procoach.security.infrastructure;

import com.fitness.procoach.security.application.AthleteInviteMailer;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envio do convite do atleta por SMTP (T-064), usando a mesma configuração
 * {@code spring.mail.*} dos avisos. Desligado por padrão
 * ({@code procoach.security.invite.email.enabled=false}); sem servidor SMTP o
 * canal fica inerte e o coach continua podendo copiar o link.
 */
@Component
public class SmtpAthleteInviteMailer implements AthleteInviteMailer {

	static final String SUBJECT = "ProCoach: convite para acessar seu acompanhamento";

	private static final Logger log = LoggerFactory.getLogger(SmtpAthleteInviteMailer.class);

	private final ObjectProvider<JavaMailSender> mailSender;
	private final boolean enabled;
	private final String from;
	private final String baseUrl;

	public SmtpAthleteInviteMailer(ObjectProvider<JavaMailSender> mailSender,
			@Value("${procoach.security.invite.email.enabled:false}") boolean enabled,
			@Value("${procoach.security.invite.email.from:no-reply@procoach.local}") String from,
			@Value("${procoach.security.invite.base-url:http://localhost:5173}") String baseUrl) {
		this.mailSender = mailSender;
		this.enabled = enabled;
		this.from = from;
		this.baseUrl = baseUrl;
	}

	@Override
	public boolean available() {
		return enabled && mailSender.getIfAvailable() != null;
	}

	@Override
	public boolean send(InviteEmail email) {
		if (!available()) {
			return false;
		}
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(from);
			message.setTo(email.recipient());
			message.setSubject(SUBJECT);
			message.setText(bodyFor(email));
			mailSender.getObject().send(message);
			return true;
		}
		catch (RuntimeException ex) {
			// O convite continua válido; o coach recebe o link na resposta da API.
			log.warn("Falha ao enviar o convite do atleta {} por e-mail", email.athleteId(), ex);
			return false;
		}
	}

	private String bodyFor(InviteEmail email) {
		String link = baseUrl + "/convite/" + email.token();
		return """
				Olá%s!

				Seu coach criou um convite para você acessar o ProCoach.
				Defina sua senha pelo link abaixo (válido até %s):
				%s

				Se você não esperava este convite, ignore esta mensagem.
				""".formatted(greeting(email.athleteName()), DateTimeFormatter.ISO_INSTANT.format(email.expiresAt()),
				link);
	}

	private static String greeting(String athleteName) {
		return athleteName == null || athleteName.isBlank() ? "" : ", " + athleteName;
	}
}
