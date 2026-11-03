/**
 * API pública do módulo athlete (T-058): {@code AthleteAccessService} é a
 * fachada que os demais módulos (assessment/nutrition/training) usam para
 * verificar se um atleta pertence ao coach autenticado — respeitando o
 * boundary do Modulith (nunca acessam {@code Athlete}/{@code AthleteRepository}
 * diretamente).
 */
@org.springframework.modulith.NamedInterface("api")
package com.fitness.procoach.athlete.application;
