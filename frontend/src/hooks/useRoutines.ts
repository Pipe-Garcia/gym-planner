import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { activateRoutine, archiveRoutine, createFromScratch, createFromTemplate, createNextRoutine, deleteRoutine, duplicateRoutine, finishAndCreateNext, finishRoutine, getActiveRoutine, getRoutine, listRoutines, listStudentRoutines, updateRoutine } from "@/api/routines"
import { useToast } from "@/hooks/useToast"
import type { CreateNextRoutineInput, DuplicateRoutineInput, FinishAndCreateNextInput, RoutineInput, RoutineListParams } from "@/types/training"

export function useStudentRoutines(studentId?: number, params: RoutineListParams = {}) {
  return useQuery({ queryKey: ["students", studentId, "routines", params], queryFn: () => listStudentRoutines(studentId!, params), enabled: Boolean(studentId) })
}

export function useRoutines(params: RoutineListParams = {}) {
  return useQuery({ queryKey: ["routines", "list", params], queryFn: () => listRoutines(params) })
}

export function useActiveRoutine(studentId?: number) {
  return useQuery({ queryKey: ["students", studentId, "routines", "active"], queryFn: () => getActiveRoutine(studentId!), enabled: Boolean(studentId), retry: false })
}

export function useRoutine(id?: number) {
  return useQuery({ queryKey: ["routines", id], queryFn: () => getRoutine(id!), enabled: Boolean(id) })
}

export function useCreateRoutineFromScratch(studentId: number) {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: (data: RoutineInput) => createFromScratch({ ...data, studentId }), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["students", studentId, "routines"] }) })
}

export function useCreateRoutineFromTemplate(studentId: number) {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: createFromTemplate, onSuccess: () => queryClient.invalidateQueries({ queryKey: ["students", studentId, "routines"] }) })
}

export function useUpdateRoutine(id: number, studentId?: number) {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: (data: RoutineInput) => updateRoutine(id, data), onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ["routines", id] })
    if (studentId) queryClient.invalidateQueries({ queryKey: ["students", studentId, "routines"] })
  } })
}

export function useRoutineAction(studentId?: number) {
  const finish = useFinishRoutine(studentId)
  const archive = useArchiveRoutine(studentId)
  const activate = useActivateRoutine(studentId)
  const deleteDraft = useDeleteRoutine(studentId)
  return { finish, archive, activate, deleteDraft }
}

export function useFinishRoutine(studentId?: number) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const invalidate = () => {
    invalidateRoutineQueries(queryClient, studentId)
  }
  return useMutation({
    mutationFn: (input: number | { routineId: number; closureNotes?: string }) => {
      const payload = typeof input === "number" ? { routineId: input } : input
      return finishRoutine(payload.routineId, { closureNotes: payload.closureNotes })
    },
    onSuccess: invalidate,
    onError: (error) => toast.error(apiErrorMessage(error, "No pudimos finalizar la rutina.")),
  })
}

export function useActivateRoutine(studentId?: number) {
  const queryClient = useQueryClient()
  const toast = useToast()
  return useMutation({
    mutationFn: activateRoutine,
    onSuccess: () => invalidateRoutineQueries(queryClient, studentId),
    onError: (error) => toast.error(apiErrorMessage(error, "No pudimos activar la rutina.")),
  })
}

export function useArchiveRoutine(studentId?: number) {
  const queryClient = useQueryClient()
  const toast = useToast()
  return useMutation({
    mutationFn: archiveRoutine,
    onSuccess: () => invalidateRoutineQueries(queryClient, studentId),
    onError: (error) => toast.error(apiErrorMessage(error, "No pudimos archivar la rutina.")),
  })
}

export function useDeleteRoutine(studentId?: number) {
  const queryClient = useQueryClient()
  const toast = useToast()
  return useMutation({
    mutationFn: deleteRoutine,
    onSuccess: () => invalidateRoutineQueries(queryClient, studentId),
    onError: (error) => toast.error(apiErrorMessage(error, "No pudimos eliminar la rutina.")),
  })
}

export function useFinishAndCreateNext(studentId?: number) {
  const queryClient = useQueryClient()
  const toast = useToast()
  return useMutation({
    mutationFn: (data: FinishAndCreateNextInput) => finishAndCreateNext(data),
    onSuccess: () => invalidateRoutineQueries(queryClient, studentId),
    onError: (error) => toast.error(apiErrorMessage(error, "No pudimos crear el próximo ciclo.")),
  })
}

export function useCreateNextRoutine(studentId?: number) {
  const queryClient = useQueryClient()
  const toast = useToast()
  return useMutation({
    mutationFn: ({ sourceRoutineId, data }: { sourceRoutineId: number; data: CreateNextRoutineInput }) => createNextRoutine(sourceRoutineId, data),
    onSuccess: () => invalidateRoutineQueries(queryClient, studentId),
    onError: (error) => toast.error(apiErrorMessage(error, "No pudimos crear el próximo ciclo.")),
  })
}

export function useDuplicateRoutine() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: DuplicateRoutineInput }) => duplicateRoutine(id, data),
    onSuccess: (routine) => {
      queryClient.invalidateQueries({ queryKey: ["routines"] })
      queryClient.invalidateQueries({ queryKey: ["students", routine.studentId, "routines"] })
    },
  })
}

function invalidateRoutineQueries(queryClient: ReturnType<typeof useQueryClient>, studentId?: number) {
  queryClient.invalidateQueries({ queryKey: ["routines"] })
  if (studentId) {
    queryClient.invalidateQueries({ queryKey: ["students", studentId, "routines"] })
  }
}

function apiErrorMessage(error: unknown, fallback: string) {
  if (error && typeof error === "object" && "response" in error) {
    const response = error.response
    if (response && typeof response === "object" && "data" in response) {
      const data = response.data
      if (data && typeof data === "object" && "message" in data && typeof data.message === "string") return data.message
    }
  }
  return error instanceof Error ? error.message : fallback
}
