import type { PageResponse } from "@/types/api"
import type { Exercise, MeasurementType } from "@/types/exercise"

export type BlockStructuralType = "STANDARD" | "CIRCUIT" | "GROUPED_SET" | "PYRAMID" | "REVERSE_PYRAMID" | "DROP_SET" | "REST_PAUSE" | "CLUSTER"
export type EditableBlockStructuralType = BlockStructuralType
export type BlockPurpose = "WARMUP" | "ACTIVATION" | "MAIN_LIFT" | "ACCESSORY" | "CONDITIONING" | "CORE" | "COOLDOWN" | "OTHER"
export type SetKind = "NORMAL" | "WARMUP" | "FAILURE" | "DROP" | "REST_PAUSE_PORTION"
export type RoutineStatus = "ACTIVE" | "FINISHED" | "ARCHIVED" | "DRAFT"

export interface ExerciseSetInput {
  setNumber: number
  setKind: SetKind
  targetReps: number | null
  targetRepsMin?: number | null
  targetRepsMax?: number | null
  targetWeightKg: number | null
  targetTimeSeconds: number | null
  targetDistanceMeters?: number | null
  restAfterSeconds: number | null
  tempo: string | null
  executionCue?: string | null
  rpe: number | null
  notes: string | null
  toFailure: boolean
}

export interface ExerciseInBlockInput {
  exerciseId: number
  exerciseName?: string
  exerciseMeasurement: MeasurementType
  orderIndex: number
  exerciseNotes: string | null
  sets: ExerciseSetInput[]
}

export interface BlockInput {
  orderIndex: number
  title: string
  structuralType: EditableBlockStructuralType
  purpose: BlockPurpose | null
  totalDurationSeconds: number | null
  targetRounds: number | null
  roundRestSeconds: number | null
  blockNotes: string | null
  exercises: ExerciseInBlockInput[]
}

export interface DayInput {
  id?: number
  orderIndex: number
  name: string
  notes: string | null
  blocks: BlockInput[]
}

export interface TemplateInput {
  name: string
  description: string | null
  sport: string | null
  objective: string | null
  level: string | null
  estimatedDurationMinutes: number | null
  generalNotes: string | null
  days: DayInput[]
}

export interface RoutineInput {
  studentId?: number
  name: string
  objective: string | null
  status?: RoutineStatus
  assignedDate: string
  finishedDate?: string | null
  generalNotes: string | null
  internalNotes: string | null
  days: DayInput[]
}

export interface DuplicateRoutineInput {
  targetStudentId: number
  name?: string | null
  assignedDate?: string | null
  status?: RoutineStatus | null
}

export interface ExerciseSet extends ExerciseSetInput { id: number }
export interface ExerciseInBlock extends ExerciseInBlockInput { id: number; exerciseName: string; exerciseActive: boolean; defaultMeasurement?: MeasurementType; exercise?: Exercise; sets: ExerciseSet[] }
export interface TrainingBlock extends BlockInput { id: number; exercises: ExerciseInBlock[] }
export interface TrainingDay extends DayInput { id: number; blocks: TrainingBlock[] }

export interface TemplateSummary {
  id: number
  name: string
  description?: string | null
  sport?: string | null
  objective?: string | null
  level?: string | null
  estimatedDurationMinutes?: number | null
  active: boolean
  dayCount: number
  blockCount: number
  exerciseCount: number
  createdAt: string
  updatedAt: string
}

export interface Template extends TemplateSummary {
  generalNotes?: string | null
  createdByUserId: number
  days: TrainingDay[]
}

export interface RoutineSummary {
  id: number
  studentId: number
  studentName: string
  name: string
  objective?: string | null
  sourceTemplateId?: number | null
  sourceTemplateName?: string | null
  status: RoutineStatus
  assignedDate: string
  finishedDate?: string | null
  finishedAt?: string | null
  closureNotes?: string | null
  previousRoutineId?: number | null
  dayCount: number
  blockCount: number
  exerciseCount: number
  createdAt: string
  updatedAt: string
}

export interface Routine extends RoutineSummary {
  generalNotes?: string | null
  internalNotes?: string | null
  createdByUserId: number
  days: TrainingDay[]
}

export type RoutineResponse = Routine
export type RoutineSummaryResponse = RoutineSummary

export interface FinishAndCreateNextInput {
  routineId: number
  closureNotes?: string
  newRoutineName?: string
  newAssignedDate: string
  newStatus: "DRAFT" | "ACTIVE"
  copyGeneralNotes: boolean
  copyInternalNotes: boolean
  weightAdjustment?: {
    percentage: number
    roundingStepKg?: number
  }
}

export interface FinishAndCreateNextResponse {
  finishedRoutine: RoutineSummaryResponse
  newRoutine: RoutineResponse
  weightSetsAdjusted: number
}

export type CreateNextRoutineInput = Omit<FinishAndCreateNextInput, "routineId" | "closureNotes">

export interface CreateNextRoutineResponse {
  sourceRoutine: RoutineSummaryResponse
  newRoutine: RoutineResponse
  weightSetsAdjusted: number
}

export interface TemplateListParams { search?: string; sport?: string; objective?: string; level?: string; active?: boolean; page?: number; size?: number; sort?: string }
export interface RoutineListParams { status?: string; q?: string; dateFrom?: string; dateTo?: string; sport?: string; level?: string; page?: number; size?: number; sort?: string }
export type TemplatePage = PageResponse<TemplateSummary>
export type RoutinePage = PageResponse<RoutineSummary>
