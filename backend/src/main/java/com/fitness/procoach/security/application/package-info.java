/**
 * API pública do módulo security (T-058): {@code AthleteAccessGuard} e
 * {@code CoachAthleteAccessGuard} são as fachadas que os demais módulos
 * (assessment/nutrition/training) usam para validação de acesso e posse de
 * atletas — respeitando o boundary do Modulith.
 */
@org.springframework.modulith.NamedInterface("api")
package com.fitness.procoach.security.application;