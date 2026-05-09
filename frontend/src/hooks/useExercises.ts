import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  createExercise,
  deactivateExercise,
  getExercise,
  listExercises,
  listTags,
  reactivateExercise,
  updateExercise,
} from "@/api/exercises"
import type { ExerciseListParams, TagType, UpdateExerciseInput } from "@/types/exercise"

export function useExercises(params: ExerciseListParams) {
  return useQuery({
    queryKey: ["exercises", params],
    queryFn: () => listExercises(params),
  })
}

export function useExercise(id?: number) {
  return useQuery({
    queryKey: ["exercises", id],
    queryFn: () => getExercise(id!),
    enabled: Boolean(id),
  })
}

export function useExerciseTags(type?: TagType) {
  return useQuery({
    queryKey: ["exercise-tags", type],
    queryFn: () => listTags(type),
  })
}

export function useCreateExercise() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createExercise,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["exercises"] }),
  })
}

export function useUpdateExercise(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: UpdateExerciseInput) => updateExercise(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["exercises"] })
      queryClient.invalidateQueries({ queryKey: ["exercises", id] })
    },
  })
}

export function useDeactivateExercise() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deactivateExercise,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["exercises"] }),
  })
}

export function useReactivateExercise() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: reactivateExercise,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["exercises"] }),
  })
}
