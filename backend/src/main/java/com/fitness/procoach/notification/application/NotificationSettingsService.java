package com.fitness.procoach.notification.application;

import com.fitness.procoach.notification.domain.DevicePlatform;
import com.fitness.procoach.notification.domain.DeviceToken;
import com.fitness.procoach.notification.domain.DeviceTokenRepository;
import com.fitness.procoach.notification.domain.NotificationPreference;
import com.fitness.procoach.notification.domain.NotificationPreferenceRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Preferências de canal e dispositivos de push do atleta. A autorização (o próprio atleta) é do chamador. */
@Service
public class NotificationSettingsService {

	private final NotificationPreferenceRepository preferences;
	private final DeviceTokenRepository devices;

	public NotificationSettingsService(NotificationPreferenceRepository preferences, DeviceTokenRepository devices) {
		this.preferences = preferences;
		this.devices = devices;
	}

	/** Preferências do atleta; sem registro, todos os canais ligados. */
	public record Preferences(boolean emailEnabled, boolean pushEnabled) {

		static Preferences of(NotificationPreference p) {
			return new Preferences(p.isEmailEnabled(), p.isPushEnabled());
		}
	}

	@Transactional(readOnly = true)
	public Preferences getPreferences(UUID athleteId) {
		return Preferences.of(preferences.findByAthleteId(athleteId).orElse(NotificationPreference.defaults(athleteId)));
	}

	@Transactional
	public Preferences updatePreferences(UUID athleteId, boolean emailEnabled, boolean pushEnabled) {
		return Preferences.of(
				preferences.save(NotificationPreference.of(athleteId, emailEnabled, pushEnabled, Instant.now())));
	}

	/** Registra o dispositivo; token já conhecido muda de dono/plataforma em vez de duplicar. */
	@Transactional
	public void registerDevice(UUID athleteId, String token, DevicePlatform platform) {
		Instant now = Instant.now();
		DeviceToken device = devices.findByToken(token).orElse(null);
		if (device == null) {
			devices.save(DeviceToken.register(athleteId, token, platform, now));
		}
		else {
			device.seenBy(athleteId, platform, now);
			devices.save(device);
		}
	}

	/** Remove o dispositivo do atleta (logout no aparelho); token de outro atleta não é tocado. */
	@Transactional
	public void unregisterDevice(UUID athleteId, String token) {
		devices.findByToken(token).filter(d -> d.getAthleteId().equals(athleteId)).ifPresent(devices::delete);
	}
}
