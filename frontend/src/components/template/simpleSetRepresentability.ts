import { emptySet } from "./formDefaults"
import { normalizeExecutionCueValue, normalizeNullableTextValue, type SetFormValue } from "@/schemas/template.schema"
import type { MeasurementType } from "@/types/exercise"

type Context = "template" | "routine"
type Classification = "simple" | "advanced" | "structural"
type Classifier = (measurement: MeasurementType, context: Context) => Classification

const simpleIf = (condition: (measurement: MeasurementType, context: Context) => boolean): Classifier =>
  (measurement, context) => condition(measurement, context) ? "simple" : "advanced"
const advanced: Classifier = () => "advanced"

// Every SetFormValue property must be classified when the schema changes.
const classification: Record<keyof SetFormValue, Classifier> = {
  setNumber: () => "structural",
  setKind: advanced,
  targetReps: simpleIf((measurement) => ["REPS_ONLY", "REPS_WEIGHT", "CIRCUIT_REPS"].includes(measurement)),
  targetRepsMin: advanced,
  targetRepsMax: advanced,
  targetWeightKg: simpleIf((measurement, context) => measurement === "REPS_WEIGHT" && context === "routine"),
  targetTimeSeconds: simpleIf((measurement) => measurement === "TIME"),
  targetDistanceMeters: simpleIf((measurement) => measurement === "DISTANCE"),
  restAfterSeconds: simpleIf((measurement) => measurement !== "CIRCUIT_REPS"),
  tempo: advanced,
  executionCue: advanced,
  rpe: advanced,
  notes: advanced,
  toFailure: advanced,
}

function comparableValue(key: keyof SetFormValue, value: unknown) {
  if (key === "executionCue") return normalizeExecutionCueValue(value)
  if (key === "notes" || key === "tempo") return normalizeNullableTextValue(value)
  return value ?? null
}

export function isSimpleRepresentable(sets: SetFormValue[], measurement: MeasurementType, context: Context): boolean {
  if (sets.length === 0 || (measurement === "CIRCUIT_REPS" && sets.length !== 1)) return false
  const neutral = emptySet(1)
  const first = sets[0]
  for (const set of sets) {
    for (const key of Object.keys(classification) as (keyof SetFormValue)[]) {
      const kind = classification[key](measurement, context)
      if (kind === "structural") continue
      const value = comparableValue(key, set[key])
      const expected = comparableValue(key, kind === "simple" ? first[key] : neutral[key])
      if (value !== expected) return false
    }
  }
  return true
}
