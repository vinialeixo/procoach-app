package com.fitness.procoach.training.application;

import com.fitness.procoach.notification.application.NotificationService;
import com.fitness.procoach.training.domain.model.Training;
import com.fitness.procoach.training.domain.model.TrainingStatus;
import org.springframework.stereotype.Component;

/**
 * Avisa o atleta quando o coach mexe em um treino que ele já enxerga (ACTIVE). Treino em
 * rascunho ou em revisão não é visível ao atleta, então não gera aviso. O aviso entra na
 * transação do caso de uso que o chamou.
 */
@Component
public class TrainingNotifier {

	public static final String TYPE_TRAINING_CHANGED = "TRAINING_CHANGED";

	private final NotificationService notificationService;

	public TrainingNotifier(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	public void trainingChanged(Training training, String message) {
		if (training.getStatus() != TrainingStatus.ACTIVE) {
			return;
		}
		notificationService.notifyAthlete(training.getAthleteId(), TYPE_TRAINING_CHANGED, message, "Training",
				training.getId());
	}
}
