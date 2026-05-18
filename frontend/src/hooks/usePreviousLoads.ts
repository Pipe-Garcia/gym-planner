import { useQuery } from "@tanstack/react-query"
import { getPreviousLoads } from "@/api/previousLoads"
import type { BlockStructuralType } from "@/types/training"

export function usePreviousLoads(args: {
  studentId?: number | null
  exerciseId?: number | null
  excludeRoutineId?: number | null
  structuralType?: BlockStructuralType | null
  includeFallback?: boolean
  limit?: number
  enabled?: boolean
}) {
  const limit = args.limit ?? 1
  const studentId = args.studentId
  const exerciseId = args.exerciseId
  const enabled =
    args.enabled !== false &&
    typeof studentId === "number" &&
    Number.isFinite(studentId) &&
    studentId > 0 &&
    typeof exerciseId === "number" &&
    Number.isFinite(exerciseId) &&
    exerciseId > 0

  return useQuery({
    queryKey: ["previous-loads", studentId ?? null, exerciseId ?? null, args.excludeRoutineId ?? null, args.structuralType ?? null, args.includeFallback ?? false, limit],
    queryFn: () => getPreviousLoads(studentId!, exerciseId!, { excludeRoutineId: args.excludeRoutineId, structuralType: args.structuralType, includeFallback: args.includeFallback, limit }),
    enabled,
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
    retry: 1,
  })
}
