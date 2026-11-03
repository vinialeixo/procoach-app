package com.fitness.procoach.notification.infrastructure;

import com.fitness.procoach.notification.application.AthleteContactProvider;
import com.fitness.procoach.notification.application.NotificationChannel;
import com.fitness.procoach.notification.domain.Notification;
import com.fitness.procoach.notification.domain.NotificationChannelType;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * E-mail do aviso ao atleta, por SMTP (spring.mail.*). Desligado por padrão
 * ({@code procoach.notification.email.enabled=false}) e inerte sem servidor SMTP configurado.
 * Só o texto do aviso vai no corpo — nada além do que o atleta já vê no app.
 */
@Component
public class EmailNotificationChannel implements NotificationChannel {

	static final String SUBJECT = "ProCoach: novo aviso do seu coach";

	private final ObjectProvider<JavaMailSender> mailSender;
	private final AthleteContactProvider contacts;
	private final boolean enabled;
	private final String from;

	public EmailNotificationChannel(ObjectProvider<JavaMailSender> mailSender, AthleteContactProvider contacts,
			@Value("${procoach.notification.email.enabled:false}") boolean enabled,
			@Value("${procoach.notification.email.from:no-reply@procoach.local}") String from) {
		this.mailSender = mailSender;
		this.contacts = contacts;
		this.enabled = enabled;
		this.from = from;
	}

	@Override
	public NotificationChannelType type() {
		return NotificationChannelType.EMAIL;
	}

	@Override
	public boolean available() {
		return enabled && mailSender.getIfAvailable() != null;
	}

	@Override
	public Result send(Notification notification) {
		Optional<String> email = contacts.emailOf(notification.getAthleteId());
		if (email.isEmpty()) {
			return Result.skipped("atleta sem e-mail de acesso");
		}
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(email.get());
		message.setSubject(SUBJECT);
		message.setText(notification.getMessage() + "\n\nAbra o ProCoach para ver os detalhes.");
		mailSender.getObject().send(message);
		return Result.sent();
	}
}
