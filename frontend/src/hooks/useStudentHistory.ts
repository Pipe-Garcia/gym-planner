import { useQuery } from "@tanstack/react-query"
import {
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
