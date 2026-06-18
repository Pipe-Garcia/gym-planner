import type { PageResponse } from "@/types/api"

export type MeasurementType = "REPS_WEIGHT" | "REPS_ONLY" | "TIME" | "DISTANCE" | "CIRCUIT_REPS"

export type TagType = "BODY_AREA" | "MUSCLE_GROUP" | "MOVEMENT_PATTERN" | "OBJECTIVE" | "LEVEL" | "EQUIPMENT"

export interface ExerciseTag {
  id: number
  type: TagType
  name: string
  slug: string
}

export interface ExerciseTagUsage extends ExerciseTag {
  usageCount: number
}

export interface ExerciseSummary {
  id: number
  name: string
  slug: string
  defaultMeasurement: MeasurementType
  active: boolean
  tags: ExerciseTag[]
  createdAt: string
  updatedAt: string
}

export interface Exercise extends ExerciseSummary {
  description?: string | null
  technicalNotes?: string | null
  videoUrl?: string | null
  imageUrl?: string | null
}

export interface ExerciseListParams {
  search?: string
  tagIds?: number[]
  active?: boolean
  page?: number
  size?: number
  sort?: string
}

export interface CreateExerciseInput {
  name: string
  description?: string
  technicalNotes?: string
  defaultMeasurement?: MeasurementType
  videoUrl?: string
  imageUrl?: string
  tagIds?: number[]
}

export type UpdateExerciseInput = Partial<CreateExerciseInput>

export type ExercisePage = PageResponse<ExerciseSummary>

export interface CreateExerciseTagRequest {
  name: string
  type: TagType
}

export interface UpdateExerciseTagRequest {
  name: string
}
