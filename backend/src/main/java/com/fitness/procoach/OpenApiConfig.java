package com.fitness.procoach;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do OpenAPI/Swagger: contrato oficial de integração do front-end
 * com o núcleo transacional do ProCoach. A UI interativa fica em
 * {@code /swagger-ui.html} e o spec OpenAPI 3 em {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI procoachOpenApi() {
		return new OpenAPI().info(new Info()
				.title("ProCoach API")
				.version("1.0.0")
				.description("""
						API do núcleo transacional do ProCoach (monólito modular, Spring Modulith).

						Módulos disponíveis:

						- **Athlete** — cadastro e ficha de atletas (id único e imutável).
						- **Assessment** — avaliações físicas com histórico cronológico; cada avaliação é um novo registro, nunca sobrescrito.
						- **Nutrition** — gestão e versionamento de dietas. Regra inegociável (US-015): uma dieta NUNCA é sobrescrita — toda alteração gera uma nova versão encadeada (v1→v2→v3) e versões anteriores tornam-se somente leitura.
						- **Library** — biblioteca de exercícios com embasamento científico.
						- **Training** — periodizações, planos de treino e treinos; planos seguem o mesmo princípio de versionamento sem sobrescrita.

						Regras transversais: exclusões são sempre soft-delete e toda operação de escrita gera registro de auditoria (append-only).
						"""));
	}

}
