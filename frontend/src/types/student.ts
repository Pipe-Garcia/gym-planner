import type { PageResponse } from "@/types/api"

export type InjurySeverity = "LEVE" | "MODERADA" | "GRAVE"

export interface StudentSummary {
  id: number
  firstName: string
  lastName: string
  documentId?: string | null
  phone?: string | null
  email?: string | null
  sport?: string | null
  level?: string | null
  active: boolean
  startedAt?: string | null
  createdAt: string
  updatedAt: string
}

export interface Student extends StudentSummary {
  birthDate?: string | null
  objective?: string | null
  generalNotes?: string | null
  activeInjuries: StudentInjury[]
}

export interface StudentInjury {
  id: number
  bodyArea: string
  description: string
  severity: InjurySeverity
  startedAt?: string | null
  resolvedAt?: string | null
  active: boolean
  notes?: string | null
  createdAt: string
  updatedAt: string
}

export interface StudentNote {
  id: number
  content: string
  authorUserId: number
  authorName: string
  createdAt: string
  updatedAt: string
}

export interface StudentListParams {
  search?: string
  active?: boolean
  sport?: string
  level?: string
  page?: number
  size?: number
  sort?: string
}

export interface CreateStudentInput {
  firstName: string
  lastName: string
  documentId?: string
  phone?: string
  email?: string
  birthDate?: string
  sport?: string
  objective?: string
  level?: string
  generalNotes?: string
  startedAt?: string
}

export type UpdateStudentInput = Partial<CreateStudentInput>

export interface CreateInjuryInput {
  bodyArea: string
  description: string
  severity: InjurySeverity
  startedAt?: string
  notes?: string
}

export interface UpdateInjuryInput extends Partial<CreateInjuryInput> {
  active?: boolean
  resolvedAt?: string
}

export interface CreateNoteInput {
  content: string
}

export type StudentPage = PageResponse<StudentSummary>
