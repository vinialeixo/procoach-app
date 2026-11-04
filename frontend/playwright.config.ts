import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: ".",
  fullyParallel: false,
  reporter: "list",
  use: {
    baseURL: "http://127.0.0.1:5173",
    browserName: "chromium",
    headless: true,
  },
  webServer: [
    {
      command: "mvn spring-boot:run",
      cwd: "../backend",
      url: "http://127.0.0.1:8080/actuator/health",
      reuseExistingServer: !process.env.CI,
      timeout: 180_000,
      env: {
        MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository",
        SPRING_PROFILES_ACTIVE: "h2",
        SPRING_DATASOURCE_URL:
          "jdbc:h2:mem:procoach-e2e;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=YEAR",
        SPRING_DATASOURCE_DRIVER_CLASS_NAME: "org.h2.Driver",
        SPRING_FLYWAY_ENABLED: "false",
        SPRING_JPA_HIBERNATE_DDL_AUTO: "create-drop",
        PROCOACH_JWT_SECRET: "procoach-e2e-test-jwt-secret-0123456789",
        PROCOACH_MFA_ENABLED: "false",
        PROCOACH_NOTIFICATION_ASYNC: "false",
        PROCOACH_NOTIFICATION_RETRY_ENABLED: "false",
        PROCOACH_EMAIL_ENABLED: "false",
        PROCOACH_PUSH_PROVIDER: "log",
      },
    },
    {
      command: "npm run dev -- --host 127.0.0.1",
      url: "http://127.0.0.1:5173",
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
  ],
});
