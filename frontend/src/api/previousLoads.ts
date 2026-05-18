import { apiClient } from "@/api/client"
import type { PreviousLoadsParams, PreviousLoadsResponse } from "@/types/previousLoads"

export async function getPreviousLoads(
  studentId: number,
  exerciseId: number,
  params: PreviousLoadsParams = {},
): Promise<PreviousLoadsResponse> {
  const queryParams: PreviousLoadsParams = { limit: params.limit ?? 1 }
  if (params.excludeRoutineId) {
    queryParams.excludeRoutineId = params.excludeRoutineId
  }
  if (params.structuralType) {
    queryParams.structuralType = params.structuralType
  }
  if (params.includeFallback !== undefined) {
    queryParams.includeFallback = params.includeFallback
  }

  const response = await apiClient.get<PreviousLoadsResponse>(
    `/api/students/${studentId}/exercises/${exerciseId}/previous-loads`,
    { params: queryParams },
  )
  return response.data
}
