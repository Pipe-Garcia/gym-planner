import { apiClient } from "@/api/client"
import type {
  StudentExerciseHistoryPage,
  StudentExerciseOccurrencePage,
  StudentHistorySummary,
  StudentRoutineTimelinePage,
} from "@/types/studentHistory"

export async function getStudentHistorySummary(
  studentId: number,
): Promise<StudentHistorySummary> {
  const response = await apiClient.get<StudentHistorySummary>(
    `/api/students/${studentId}/history/summary`,
  )
  return response.data
}

export async function getStudentHistoryTimeline(
  studentId: number,
  params: { page?: number; size?: number } = {},
): Promise<StudentRoutineTimelinePage> {
  const response = await apiClient.get<StudentRoutineTimelinePage>(
    `/api/students/${studentId}/history/timeline`,
    { params },
  )
  return response.data
}

export async function getStudentExerciseHistory(
  studentId: number,
  params: { search?: string; page?: number; size?: number } = {},
): Promise<StudentExerciseHistoryPage> {
  const normalizedSearch = params.search?.trim()
  const response = await apiClient.get<StudentExerciseHistoryPage>(
    `/api/students/${studentId}/history/exercises`,
    {
      params: {
        page: params.page,
        size: params.size,
        ...(normalizedSearch ? { search: normalizedSearch } : {}),
      },
    },
  )
  return response.data
}

export async function getStudentExerciseOccurrences(
  studentId: number,
  exerciseId: number,
  params: { page?: number; size?: number } = {},
): Promise<StudentExerciseOccurrencePage> {
  const response = await apiClient.get<StudentExerciseOccurrencePage>(
    `/api/students/${studentId}/history/exercises/${exerciseId}/occurrences`,
    { params },
  )
  return response.data
}
