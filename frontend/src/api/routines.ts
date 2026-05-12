import { apiClient } from "@/api/client"
import type { DuplicateRoutineInput, Routine, RoutineInput, RoutineListParams, RoutinePage, RoutineStatus } from "@/types/training"

export async function listStudentRoutines(studentId: number, params: RoutineListParams = {}) {
  const response = await apiClient.get<RoutinePage>(`/api/students/${studentId}/routines`, { params })
  return response.data
}

export async function listRoutines(params: RoutineListParams = {}) {
  const response = await apiClient.get<RoutinePage>("/api/routines", { params })
  return response.data
}

export async function getActiveRoutine(studentId: number) {
  const response = await apiClient.get<Routine>(`/api/students/${studentId}/routines/active`)
  return response.data
}

export async function getRoutine(id: number) {
  const response = await apiClient.get<Routine>(`/api/routines/${id}`)
  return response.data
}

export async function createFromScratch(data: RoutineInput & { studentId: number }) {
  const response = await apiClient.post<Routine>("/api/routines/from-scratch", data)
  return response.data
}

export async function createFromTemplate(data: { studentId: number; templateId: number; name?: string | null; assignedDate?: string | null; generalNotes?: string | null; internalNotes?: string | null; status?: RoutineStatus }) {
  const response = await apiClient.post<Routine>("/api/routines/from-template", data)
  return response.data
}

export async function updateRoutine(id: number, data: RoutineInput) {
  const response = await apiClient.put<Routine>(`/api/routines/${id}`, data)
  return response.data
}

export async function duplicateRoutine(id: number, data: DuplicateRoutineInput) {
  const response = await apiClient.post<Routine>(`/api/routines/${id}/duplicate`, data)
  return response.data
}

export async function finishRoutine(id: number) {
  const response = await apiClient.post<Routine>(`/api/routines/${id}/finish`)
  return response.data
}

export async function archiveRoutine(id: number) {
  const response = await apiClient.post<Routine>(`/api/routines/${id}/archive`)
  return response.data
}

export async function activateRoutine(id: number) {
  const response = await apiClient.post<Routine>(`/api/routines/${id}/activate`)
  return response.data
}

export async function deleteRoutine(id: number) {
  await apiClient.delete(`/api/routines/${id}`)
}
