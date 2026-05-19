import type { PageResponse } from "@/types/api"
import type { RoutineStatus } from "@/types/training"

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
