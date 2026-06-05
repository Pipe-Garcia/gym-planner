import type { PageResponse } from "@/types/api"
import type { MeasurementType } from "@/types/exercise"
import type {
  BlockPurpose,
  BlockStructuralType,
  RoutineStatus,
  SetKind,
} from "@/types/training"

export type StudentHistorySummary = {
  studentId: number
  studentFullName: string
  totalRoutines: number
  activeRoutineId: number | null
  activeRoutineName: string | null
  activeRoutineAssignedDate: string | null
  activeRoutineDaysCount: number | null
  distinctExercisesCount: number
  trainingSince: string | null
}

export type StudentRoutineTimelineItem = {
  routineId: number
  routineName: string
  status: RoutineStatus
  assignedDate: string | null
  finishedDate: string | null
  durationDays: number | null
  daysCount: number
  blocksCount: number
  exercisesCount: number
  sourceTemplateId: number | null
  sourceTemplateName: string | null
  closureNotes: string | null
}

export type StudentRoutineTimelinePage = PageResponse<StudentRoutineTimelineItem> & {
  first?: boolean
  last?: boolean
}

export type StudentExerciseHistoryItem = {
  exerciseId: number
  exerciseName: string
  lastPerformedDate: string | null
  lastRoutineId: number | null
  lastRoutineName: string | null
  timesUsed: number
  structuralTypesUsed: BlockStructuralType[]
  lastStructuralType: BlockStructuralType | null
}

export type StudentExerciseHistoryPage = PageResponse<StudentExerciseHistoryItem> & {
  first?: boolean
  last?: boolean
}

export type StudentExerciseOccurrence = {
  routineId: number
  routineName: string
  routineStatus: RoutineStatus
  assignedDate: string | null
  finishedDate: string | null
  effectiveDate: string | null
  dayOrderIndex: number | null
  dayName: string | null
  blockTitle: string | null
  blockStructuralType: BlockStructuralType
  blockPurpose: BlockPurpose | null
  exerciseNotes: string | null
  measurementType: MeasurementType
  sets: StudentExerciseOccurrenceSet[]
}

export type StudentExerciseOccurrenceSet = {
  setNumber: number
  setKind: SetKind
  targetReps: number | null
  targetRepsMin: number | null
  targetRepsMax: number | null
  targetWeightKg: number | null
  targetTimeSeconds: number | null
  targetDistanceMeters: number | null
  restAfterSeconds: number | null
  tempo: string | null
  rpe: number | null
  toFailure: boolean
  notes: string | null
  executionCue: string | null
}

export type StudentExerciseOccurrencePage =
  PageResponse<StudentExerciseOccurrence> & {
    first?: boolean
    last?: boolean
  }
