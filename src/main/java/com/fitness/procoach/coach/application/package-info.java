/**
 * API pública do módulo coach (T-058): {@code CoachApplicationService} é a
 * fachada que os demais módulos (athlete/assessment/nutrition/training) usam
 * para resolver o Coach do usuário autenticado a partir do subject do JWT —
 * respeitando o boundary do Modulith (nunca acessam o domínio/infraestrutura
 * do coach diretamente).
 */
@org.springframework.modulith.NamedInterface("api")
package com.fitness.procoach.coach.application;
