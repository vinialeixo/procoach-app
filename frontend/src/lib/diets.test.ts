import { describe, expect, it } from "vitest";
import { DIET_STATUS_FLOW, DietVersion, findCurrentDietVersion } from "./diets";

function diet(overrides: Partial<DietVersion> = {}): DietVersion {
  return {
    id: "diet-1",
    coachId: "coach-1",
    athleteId: "athlete-1",
    objective: "MAINTENANCE",
    macros: { calories: 2000, protein: 120, carbohydrates: 220, fat: 65 },
    status: "DRAFT",
    version: 1,
    previousVersionId: null,
    changeReason: null,
    createdBy: "coach-1",
    updatedBy: "coach-1",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    readOnly: false,
    deleted: false,
    ...overrides,
  };
}

describe("findCurrentDietVersion", () => {
  it("returns the newest editable version regardless of input order", () => {
    const older = diet({ id: "diet-1", version: 1, readOnly: true });
    const current = diet({ id: "diet-3", version: 3, status: "IN_REVIEW" });
    const middle = diet({ id: "diet-2", version: 2, readOnly: true });

    expect(findCurrentDietVersion([older, current, middle])).toEqual(current);
  });

  it("skips deleted and read-only versions when finding the current version", () => {
    const deleted = diet({ id: "deleted", version: 4, deleted: true });
    const historical = diet({ id: "historical", version: 3, readOnly: true });
    const current = diet({ id: "current", version: 2 });

    expect(findCurrentDietVersion([deleted, historical, current])).toEqual(current);
  });

  it("returns undefined when no editable version exists", () => {
    expect(findCurrentDietVersion([
      diet({ readOnly: true }),
      diet({ id: "deleted", version: 2, deleted: true }),
    ])).toBeUndefined();
  });
});

describe("DIET_STATUS_FLOW", () => {
  it.each([
    ["DRAFT", "IN_REVIEW"],
    ["IN_REVIEW", "APPROVED"],
    ["APPROVED", "ARCHIVED"],
    ["ARCHIVED", null],
  ] as const)("maps %s to its only next status", (status, nextStatus) => {
    expect(DIET_STATUS_FLOW[status]).toBe(nextStatus);
  });
});
