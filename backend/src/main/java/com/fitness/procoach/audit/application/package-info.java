/**
 * API pública do módulo audit (T-013/T-015): {@code AuditService} registra o que
 * mudou, quem mudou e quando, e {@code AuditData} monta o payload. O audit_log é
 * append-only (trigger no banco) e entra na transação do módulo que o chama.
 */
@org.springframework.modulith.NamedInterface("api")
package com.fitness.procoach.audit.application;
