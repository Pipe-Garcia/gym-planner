import { useQuery } from "@tanstack/react-query"
import {
  getStudentExerciseHistory,
  getStudentExerciseOccurrences,
  getStudentHistorySummary,
  getStudentHistoryTimeline,
} from "@/api/studentHistory"

const staleTime = 5 * 60 * 1000
const gcTime = 10 * 60 * 1000

export function useStudentHistorySummary(studentId?: number | null) {
  return useQuery({
    queryKey: ["student-history-summary", studentId],
    queryFn: () => getStudentHistorySummary(studentId!),
    enabled: Boolean(studentId),
    staleTime,
    gcTime,
    retry: 1,
  })
}

export function useStudentHistoryTimeline({
  studentId,
  page = 0,
  size = 10,
  enabled = true,
}: {
  studentId?: number | null
  page?: number
  size?: number
  enabled?: boolean
}) {
  return useQuery({
    queryKey: ["student-history-timeline", studentId, page, size],
    queryFn: () => getStudentHistoryTimeline(studentId!, { page, size }),
    enabled: Boolean(studentId) && enabled,
    staleTime,
    gcTime,
    retry: 1,
  })
}

export function useStudentExerciseHistory({
  studentId,
  search = "",
  page = 0,
  size = 20,
  enabled = true,
}: {
  studentId?: number | null
  search?: string
  page?: number
  size?: number
  enabled?: boolean
}) {
  const normalizedSearch = search.trim()

  return useQuery({
    queryKey: [
      "student-history-exercises",
      studentId,
      normalizedSearch,
      page,
      size,
    ],
    queryFn: () =>
      getStudentExerciseHistory(studentId!, {
        search: normalizedSearch,
        page,
        size,
      }),
    enabled: Boolean(studentId) && enabled,
    staleTime,
    gcTime,
    retry: 1,
  })
}

export function useStudentExerciseOccurrences({
  studentId,
  exerciseId,
  page = 0,
  size = 10,
  enabled = true,
}: {
  studentId?: number | null
  exerciseId?: number | null
  page?: number
  size?: number
  enabled?: boolean
}) {
  return useQuery({
    queryKey: [
      "student-history-exercise-occurrences",
      studentId,
      exerciseId,
      page,
      size,
    ],
    queryFn: () =>
      getStudentExerciseOccurrences(studentId!, exerciseId!, { page, size }),
    enabled: Boolean(studentId) && Boolean(exerciseId) && enabled,
    staleTime,
    gcTime,
    retry: 1,
  })
}
