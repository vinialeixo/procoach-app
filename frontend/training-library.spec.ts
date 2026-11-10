import { expect, test } from "@playwright/test";

const backendUrl = "http://127.0.0.1:8080";

test("a tela T-040 consulta a biblioteca e grava uma prescrição no backend", async ({
  page,
  request,
}) => {
  const unique = `${Date.now()}-${Math.floor(Math.random() * 1_000_000)}`;
  const libraryExerciseName = `Supino E2E ${unique}`;
  const registration = await request.post(`${backendUrl}/api/v1/auth/register`, {
    data: {
      name: "Coach de integração",
      email: `training-e2e-${unique}@test.com`,
      password: "senha123!",
    },
  });
  expect(registration.status()).toBe(201);
  const { accessToken } = await registration.json();

  const athleteResponse = await request.post(`${backendUrl}/api/v1/athletes`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: {
      name: "Atleta de integração",
      sex: "FEMALE",
      birthDate: "1995-07-20",
      height: 165.5,
      weight: 60.2,
      goal: "HYPERTROPHY",
    },
  });
  expect(athleteResponse.status()).toBe(201);
  const athlete = await athleteResponse.json();

  const exerciseResponse = await request.post(`${backendUrl}/api/library/exercises`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: {
      name: libraryExerciseName,
      muscleGroups: ["PEITO", "TRICEPS"],
      equipment: "BARRA_LIVRE",
      execution: "Desça a barra com controle e empurre.",
      commonErrors: ["Arquear a lombar"],
      progressions: ["Aumentar carga"],
      regressions: ["Reduzir carga"],
      contraindications: [],
      references: [],
    },
  });
  expect(exerciseResponse.status()).toBe(201);

  await page.addInitScript((token) => {
    window.localStorage.setItem("procoach.token", token);
  }, accessToken);
  await page.goto("/coach/training");
  await expect(page.getByRole("heading", { name: "Treinos e biblioteca" })).toBeVisible();

  const athleteSelect = page.getByLabel("Atleta", { exact: true });
  await expect(athleteSelect).toBeEnabled();
  await athleteSelect.selectOption(athlete.id);

  const periodizationForm = page.locator("form").filter({
    has: page.getByRole("heading", { name: "Criar periodização" }),
  });
  await periodizationForm.getByLabel("Nome da periodização").fill("Ciclo integração");
  await periodizationForm.getByLabel("Data de início").fill("2026-11-03");
  await periodizationForm.getByLabel("Data de término").fill("2026-12-31");
  await periodizationForm.getByRole("button", { name: "Criar periodização" }).click();

  const planForm = page.locator("form").filter({
    has: page.getByRole("heading", { name: "Criar plano v1" }),
  });
  await planForm.getByLabel("Nome do plano").fill("Plano integração");
  await planForm.getByRole("button", { name: "Criar plano" }).click();

  const sessionForm = page.locator("form").filter({
    has: page.getByRole("heading", { name: "Adicionar sessão ao cronograma" }),
  });
  await sessionForm.getByLabel("Nome da sessão").fill("Treino integração");
  await sessionForm.getByLabel("Semana").fill("1");
  await sessionForm.getByLabel("Dia").selectOption("1");
  await sessionForm.getByRole("button", { name: "Adicionar sessão" }).click();
  await expect(page.getByRole("heading", { name: "Treino integração" })).toBeVisible();

  await page.getByLabel("Buscar exercício").fill(libraryExerciseName);
  await page.getByLabel("Grupo muscular").selectOption("PEITO");
  const libraryRequest = page.waitForRequest((requestEvent) => {
    const url = new URL(requestEvent.url());
    return url.pathname === "/api/library/exercises"
      && url.searchParams.get("name") === libraryExerciseName
      && url.searchParams.get("muscleGroup") === "PEITO";
  });
  await page.getByRole("button", { name: "Buscar" }).click();
  await libraryRequest;

  await page.getByRole("button", { name: new RegExp(libraryExerciseName) }).click();
  await page.getByLabel("Séries").fill("4");
  await page.getByLabel("Repetições").fill("10");
  await page.getByLabel("Carga (kg)").fill("60");
  await page.getByLabel("Intensidade", { exact: true }).selectOption("RPE");
  await page.getByLabel("Valor da intensidade").fill("8");
  await page.getByLabel("Descanso (segundos)").fill("90");
  await page.getByRole("button", { name: "Adicionar à sessão" }).click();

  await expect(page.locator(".training-page__prescriptions")).toContainText(libraryExerciseName);
  await expect(page.locator(".training-page__prescriptions")).toContainText("4 séries × 10 reps");
});
