import { afterEach, describe, expect, it } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClientProvider, useQuery } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import {
  clearForbiddenError,
  createQueryClient,
  shouldRetryQuery,
} from "./query-client";
import { GlobalForbiddenBanner } from "@/components/GlobalForbiddenBanner";

afterEach(() => {
  clearForbiddenError();
});

describe("query-client", () => {
  it("does not retry 401, 403, or 404 errors, but retries 500 up to 2 times", () => {
    expect(shouldRetryQuery(0, { response: { status: 401 } })).toBe(false);
    expect(shouldRetryQuery(0, { response: { status: 403 } })).toBe(false);
    expect(shouldRetryQuery(0, { response: { status: 404 } })).toBe(false);

    expect(shouldRetryQuery(0, { response: { status: 500 } })).toBe(true);
    expect(shouldRetryQuery(1, { response: { status: 500 } })).toBe(true);
    expect(shouldRetryQuery(2, { response: { status: 500 } })).toBe(false);
  });

  it("configures a reasonable default staleTime", () => {
    const client = createQueryClient();
    expect(client.getDefaultOptions().queries?.staleTime).toBe(60_000);
  });

  it("displays and dismisses GlobalForbiddenBanner on global 403 query errors", async () => {
    const client = createQueryClient();

    function FailingComponent() {
      useQuery({
        queryKey: ["forbidden-resource"],
        queryFn: async () => {
          throw {
            response: {
              status: 403,
              data: { message: "Sem permissão para acessar este atleta." },
            },
          };
        },
      });
      return <div>Conteúdo</div>;
    }

    render(
      <QueryClientProvider client={client}>
        <MemoryRouter>
          <GlobalForbiddenBanner />
          <FailingComponent />
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByRole("alert")).toBeInTheDocument();
    });
    expect(screen.getByText("Acesso negado")).toBeInTheDocument();
    expect(
      screen.getByText("Sem permissão para acessar este atleta.")
    ).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Fechar" }));
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
});
