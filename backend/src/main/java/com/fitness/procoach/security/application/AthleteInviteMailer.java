package com.fitness.procoach.security.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Porta de envio do convite do atleta por e-mail (T-064). O token só existe em
 * memória no momento da emissão — a implementação monta o link e o entrega ao
 * atleta; o hash continua sendo a única coisa persistida.
 *
 * <p>Fica em {@code application} para que o serviço de convite não dependa de
 * SMTP/Spring Mail, e é implementada na infraestrutura do próprio módulo.</p>
 */
public interface AthleteInviteMailer {

	/** Dados necessários para montar a mensagem do convite. */
	record InviteEmail(UUID athleteId, String athleteName, String recipient, String token, Instant expiresAt) {
	}

	/** {@code false} quando o SMTP não está configurado neste ambiente. */
	boolean available();

	/**
	 * Tenta entregar o convite. Devolve {@code false} quando o canal está
	 * desligado ou a entrega falha; o convite em si permanece válido e o link
	 * continua disponível para o coach repassar manualmente.
	 */
	boolean send(InviteEmail email);
}
