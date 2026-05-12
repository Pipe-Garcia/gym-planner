import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { activateRoutine, archiveRoutine, createFromScratch, createFromTemplate, deleteRoutine, duplicateRoutine, finishRoutine, getActiveRoutine, getRoutine, listRoutines, listStudentRoutines, updateRoutine } from "@/api/routines"
import type { DuplicateRoutineInput, RoutineInput, RoutineListParams } from "@/types/training"

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
  const queryClient = useQueryClient()
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["routines"] })
    if (studentId) queryClient.invalidateQueries({ queryKey: ["students", studentId, "routines"] })
  }
  return {
    finish: useMutation({ mutationFn: finishRoutine, onSuccess: invalidate }),
    archive: useMutation({ mutationFn: archiveRoutine, onSuccess: invalidate }),
    activate: useMutation({ mutationFn: activateRoutine, onSuccess: invalidate }),
    deleteDraft: useMutation({ mutationFn: deleteRoutine, onSuccess: invalidate }),
  }
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
