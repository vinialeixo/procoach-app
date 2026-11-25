package com.fitness.procoach.notification.infrastructure;

/** Fornece o token OAuth2 de acesso ao provedor de push, renovando quando expira. */
@FunctionalInterface
interface PushAccessTokenProvider {

	String accessToken();
}
