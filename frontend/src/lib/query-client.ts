import { MutationCache, QueryCache, QueryClient } from "@tanstack/react-query";

const NON_RETRYABLE_STATUSES = new Set([401, 403, 404]);
const DEFAULT_FORBIDDEN_MESSAGE =
  "Você não tem permissão para realizar esta ação ou acessar este recurso.";

type ForbiddenListener = (message: string | null) => void;

const forbiddenListeners = new Set<ForbiddenListener>();
let currentForbiddenMessage: string | null = null;

export function getHttpStatus(error: unknown): number | undefined {
  if (!error || typeof error !== "object") {
    return undefined;
  }
  const response = (error as { response?: { status?: unknown } }).response;
  return typeof response?.status === "number" ? response.status : undefined;
}

export function shouldRetryQuery(failureCount: number, error: unknown): boolean {
  const status = getHttpStatus(error);
  if (status !== undefined && NON_RETRYABLE_STATUSES.has(status)) {
    return false;
  }
  return failureCount < 2;
}

function extractForbiddenMessage(error: unknown): string {
  if (error && typeof error === "object") {
    const data = (error as { response?: { data?: { message?: unknown } } }).response?.data;
    if (typeof data?.message === "string" && data.message.trim().length > 0) {
      return data.message;
    }
  }
  return DEFAULT_FORBIDDEN_MESSAGE;
}

export function notifyForbiddenError(message = DEFAULT_FORBIDDEN_MESSAGE): void {
  currentForbiddenMessage = message;
  for (const listener of forbiddenListeners) {
    listener(currentForbiddenMessage);
  }
}

export function clearForbiddenError(): void {
  if (currentForbiddenMessage === null) {
    return;
  }
  currentForbiddenMessage = null;
  for (const listener of forbiddenListeners) {
    listener(null);
  }
}

export function getForbiddenError(): string | null {
  return currentForbiddenMessage;
}

export function subscribeForbiddenError(listener: ForbiddenListener): () => void {
  forbiddenListeners.add(listener);
  return () => {
    forbiddenListeners.delete(listener);
  };
}

export type CreateQueryClientOptions = {
  onForbidden?: (error: unknown) => void;
};

function handleGlobalError(
  error: unknown,
  onForbidden?: (error: unknown) => void
): void {
  if (getHttpStatus(error) !== 403) {
    return;
  }
  if (onForbidden) {
    onForbidden(error);
    return;
  }
  notifyForbiddenError(extractForbiddenMessage(error));
}

export function createQueryClient(
  options?: CreateQueryClientOptions
): QueryClient {
  return new QueryClient({
    queryCache: new QueryCache({
      onError: (error) => handleGlobalError(error, options?.onForbidden),
    }),
    mutationCache: new MutationCache({
      onError: (error) => handleGlobalError(error, options?.onForbidden),
    }),
    defaultOptions: {
      queries: {
        staleTime: 60_000,
        retry: shouldRetryQuery,
      },
      mutations: {
        retry: false,
      },
    },
  });
}

export const queryClient = createQueryClient();
