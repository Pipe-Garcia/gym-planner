import { apiClient } from "@/api/client"
import type {
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
