package com.fitness.procoach;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Suporte de testes de integração (T-058): registra coaches e atletas com token
 * JWT real (via HTTP), para exercitar a resolução server-side do coachId a
 * partir do subject do JWT — nunca {@code @WithMockUser}.
 */
public final class CoachAthleteTestSupport {

	private CoachAthleteTestSupport() {
	}

	public static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@test.com";
	}

	public static String bearer(String token) {
		return "Bearer " + token;
	}

	/**
	 * Registra um coach via HTTP e devolve o access token. O cadastro também
	 * cria o agregado Coach vinculado ao User (T-058).
	 */
	public static String registerCoach(MockMvc mockMvc, String email) throws Exception {
		MvcResult result = mockMvc
				.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Coach Teste", "email": "%s", "password": "senha123!"}
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessToken").isString())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
	}

	/**
	 * Registra um atleta para o coach autenticado (token real) e devolve o id do
	 * atleta criado. O coachId é resolvido server-side a partir do JWT.
	 */
	public static String registerAthlete(MockMvc mockMvc, String coachToken) throws Exception {
		MvcResult result = mockMvc
				.perform(post("/api/v1/athletes")
						.header("Authorization", bearer(coachToken))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Ana Souza",
								  "sex": "FEMALE",
								  "birthDate": "1995-07-20",
								  "height": 165.50,
								  "weight": 60.20,
								  "goal": "HYPERTROPHY"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isString())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}
}
