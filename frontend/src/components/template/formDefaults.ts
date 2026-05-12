import type { MeasurementType } from "@/types/exercise"
import type { BlockInput, DayInput, ExerciseInBlockInput, ExerciseSetInput, SetKind } from "@/types/training"

export function emptySet(setNumber = 1): ExerciseSetInput {
  return { setNumber, setKind: "NORMAL", targetReps: 10, targetRepsMin: null, targetRepsMax: null, targetWeightKg: null, targetTimeSeconds: null, targetDistanceMeters: null, restAfterSeconds: 60, tempo: null, rpe: null, notes: null, toFailure: false }
}

export function emptyBlock(orderIndex = 1): BlockInput {
  return { orderIndex, title: `Bloque ${orderIndex}`, structuralType: "STANDARD", purpose: "MAIN_LIFT", totalDurationSeconds: null, targetRounds: null, blockNotes: null, exercises: [] }
}

export function defaultDay(orderIndex: number): DayInput {
  return {
    orderIndex,
    name: orderIndex === 1 ? "Día 1" : `Día ${orderIndex}`,
    notes: null,
    blocks: [],
  }
}

export function normalizeBlockOrder<T extends { orderIndex: number }>(items: T[]) {
  return items.map((item, index) => ({ ...item, orderIndex: index + 1 }))
}

function numberOrNull(value: number | null | undefined) {
  return typeof value === "number" && !Number.isNaN(value) ? value : null
}

export function normalizeSetForSubmit(set: Partial<ExerciseSetInput>, setNumber: number, measurement: MeasurementType, context: "template" | "routine"): ExerciseSetInput {
  const usesReps = measurement === "REPS_WEIGHT" || measurement === "REPS_ONLY" || measurement === "CIRCUIT_REPS"
  return {
    setNumber,
    setKind: (set.setKind ?? "NORMAL") as SetKind,
    targetReps: usesReps ? numberOrNull(set.targetReps) : null,
    targetRepsMin: usesReps ? numberOrNull(set.targetRepsMin) : null,
    targetRepsMax: usesReps ? numberOrNull(set.targetRepsMax) : null,
    targetWeightKg: measurement === "REPS_WEIGHT" && context === "routine" ? numberOrNull(set.targetWeightKg) : null,
    targetTimeSeconds: measurement === "TIME" ? numberOrNull(set.targetTimeSeconds) : null,
    targetDistanceMeters: measurement === "DISTANCE" ? numberOrNull(set.targetDistanceMeters) : null,
    restAfterSeconds: measurement === "CIRCUIT_REPS" ? null : numberOrNull(set.restAfterSeconds),
    tempo: set.tempo ?? null,
    rpe: measurement === "TIME" || measurement === "DISTANCE" ? null : numberOrNull(set.rpe),
    notes: set.notes ?? null,
    toFailure: measurement === "TIME" || measurement === "DISTANCE" ? false : Boolean(set.toFailure),
  }
}

export function normalizeExerciseForSubmit(exercise: ExerciseInBlockInput, orderIndex: number, context: "template" | "routine"): ExerciseInBlockInput {
  const measurement = exercise.exerciseMeasurement ?? "REPS_WEIGHT"
  return {
    ...exercise,
    exerciseMeasurement: measurement,
    orderIndex,
    exerciseNotes: exercise.exerciseNotes ?? null,
    sets: exercise.sets.map((set, index) => normalizeSetForSubmit(set, index + 1, measurement, context)),
  }
}

export function normalizeBlockForSubmit(block: BlockInput, orderIndex: number, context: "template" | "routine"): BlockInput {
  const isCircuit = block.structuralType === "CIRCUIT"
  return {
    ...block,
    orderIndex,
    targetRounds: null,
    totalDurationSeconds: isCircuit ? block.totalDurationSeconds ?? null : null,
    exercises: block.exercises.map((exercise, index) => {
      const normalized = normalizeExerciseForSubmit(exercise, index + 1, context)
      return isCircuit
        ? {
            ...normalized,
            sets: normalized.sets.slice(0, 1).map((set) => ({ ...set, restAfterSeconds: null, tempo: null, rpe: null, toFailure: false })),
          }
        : normalized
    }),
  }
}
