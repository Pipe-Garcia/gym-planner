import type { MeasurementType } from "@/types/exercise"
import type { BlockPurpose, BlockStructuralType, RoutineStatus, SetKind } from "@/types/training"

export type PreviousLoadsMatchType = "SAME_STRUCTURAL_TYPE" | "DIFFERENT_STRUCTURAL_TYPE" | "NONE"

export type PreviousLoadsResponse = {
  exerciseId: number
  exerciseName: string
  found: boolean
  matchType: PreviousLoadsMatchType
  requestedStructuralType: string | null
  occurrences: PreviousLoadOccurrence[]
}

export type PreviousLoadOccurrence = {
  routineId: number
  routineName: string
  routineStatus: RoutineStatus
  assignedDate: string | null
  finishedDate: string | null
  dayOrderIndex: number
  dayName: string
  blockTitle: string
  blockStructuralType: BlockStructuralType
  blockPurpose: BlockPurpose | null
  exerciseNotes: string | null
  measurementType: MeasurementType
  sets: PreviousLoadSet[]
}

export type PreviousLoadSet = {
  setNumber: number
  setKind: SetKind
  targetReps: number | null
  targetRepsMin: number | null
  targetRepsMax: number | null
  targetWeightKg: number | null
  targetTimeSeconds: number | null
  targetDistanceMeters: number | null
  restAfterSeconds: number | null
  rpe: number | null
  toFailure: boolean
}

export type PreviousLoadsParams = {
  excludeRoutineId?: number | null
  limit?: number
  structuralType?: BlockStructuralType | null
  includeFallback?: boolean
}
