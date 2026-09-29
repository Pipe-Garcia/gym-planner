import { describe, expect, it } from "vitest"
import { emptySet } from "./formDefaults"
import { isSimpleRepresentable } from "./simpleSetRepresentability"
import type { SetFormValue } from "@/schemas/template.schema"
import type { MeasurementType } from "@/types/exercise"

const set = (overrides: Partial<SetFormValue> = {}): SetFormValue => ({ ...emptySet(1), ...overrides })

describe("isSimpleRepresentable", () => {
  it("rejects zero sets and accepts a neutral single set", () => {
    expect(isSimpleRepresentable([], "REPS_ONLY", "routine")).toBe(false)
    expect(isSimpleRepresentable([set()], "REPS_ONLY", "routine")).toBe(true)
  })

  it.each([
    ["rpe", 8], ["tempo", "3-1-1"], ["notes", "nota"], ["toFailure", true],
    ["targetRepsMin", 6], ["targetRepsMax", 10], ["executionCue", "parcial"],
    ["setKind", "DROP"],
  ] as const)("rejects advanced-only %s even on the first set", (key, value) => {
    expect(isSimpleRepresentable([set({ [key]: value })], "REPS_ONLY", "routine")).toBe(false)
  })

  it("rejects special set kinds in later sets and heterogeneous represented values", () => {
    expect(isSimpleRepresentable([set(), set({ setNumber: 2, setKind: "WARMUP" })], "REPS_ONLY", "routine")).toBe(false)
    expect(isSimpleRepresentable([set({ targetReps: 8 }), set({ setNumber: 2, targetReps: 10 })], "REPS_ONLY", "routine")).toBe(false)
    expect(isSimpleRepresentable([set({ targetReps: 8 }), set({ setNumber: 2, targetReps: 8 })], "REPS_ONLY", "routine")).toBe(true)
  })

  it("treats template weight as invisible but routine weight as represented", () => {
    const weighted = [set({ targetReps: 8, targetWeightKg: 60 })]
    expect(isSimpleRepresentable(weighted, "REPS_WEIGHT", "template")).toBe(false)
    expect(isSimpleRepresentable(weighted, "REPS_WEIGHT", "routine")).toBe(true)
  })

  it.each(["REPS_ONLY", "TIME", "DISTANCE"] as MeasurementType[])("accepts neutral %s sets", (measurement) => {
    expect(isSimpleRepresentable([set(), set({ setNumber: 2 })], measurement, "routine")).toBe(true)
  })

  it("limits CIRCUIT_REPS to a single set without rest", () => {
    expect(isSimpleRepresentable([set({ targetReps: 12 })], "CIRCUIT_REPS", "routine")).toBe(true)
    expect(isSimpleRepresentable([set(), set({ setNumber: 2 })], "CIRCUIT_REPS", "routine")).toBe(false)
    expect(isSimpleRepresentable([set({ restAfterSeconds: 30 })], "CIRCUIT_REPS", "routine")).toBe(false)
  })

  it.each([
    ["notes", ""],
    ["tempo", ""],
    ["executionCue", ""],
    ["executionCue", "   "],
  ] as const)("treats semantically empty %s as neutral", (key, value) => {
    expect(isSimpleRepresentable([set({ [key]: value })], "REPS_ONLY", "routine")).toBe(true)
  })

  it.each([
    ["notes", "   "],
    ["tempo", "   "],
    ["executionCue", "controlado"],
  ] as const)("keeps meaningful %s text advanced-only", (key, value) => {
    expect(isSimpleRepresentable([set({ [key]: value })], "REPS_ONLY", "routine")).toBe(false)
  })
})
