import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import type { AxiosError } from "axios"
import {
  createExercise,
  createExerciseTag,
  deactivateExercise,
  deleteExerciseTag,
  getExercise,
  listExerciseTags,
  listExerciseTagUsage,
  listExercises,
  reactivateExercise,
  updateExercise,
  updateExerciseTag,
} from "@/api/exercises"
import type {
  CreateExerciseTagRequest,
  ExerciseListParams,
  ExerciseTag,
  TagType,
  UpdateExerciseInput,
  UpdateExerciseTagRequest,
} from "@/types/exercise"
import type { ApiError } from "@/types/api"

export const exerciseKeys = {
  all: ["exercises"] as const,
  list: (params: ExerciseListParams) => [...exerciseKeys.all, params] as const,
  detail: (id?: number) => [...exerciseKeys.all, id] as const,
}

export const exerciseTagKeys = {
  all: ["exercise-tags"] as const,
  list: (type?: TagType) => [...exerciseTagKeys.all, "list", { type }] as const,
  usage: () => [...exerciseTagKeys.all, "usage"] as const,
}

function invalidateExerciseTagData(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: exerciseTagKeys.all })
  queryClient.invalidateQueries({ queryKey: exerciseKeys.all })
}

export function useExercises(params: ExerciseListParams) {
  return useQuery({
    queryKey: exerciseKeys.list(params),
    queryFn: () => listExercises(params),
  })
}

export function useExercise(id?: number) {
  return useQuery({
    queryKey: exerciseKeys.detail(id),
    queryFn: () => getExercise(id!),
    enabled: Boolean(id),
  })
}

export function useExerciseTags(type?: TagType) {
  return useQuery({
    queryKey: exerciseTagKeys.list(type),
    queryFn: () => listExerciseTags(type),
  })
}

export function useExerciseTagUsage() {
  return useQuery({
    queryKey: exerciseTagKeys.usage(),
    queryFn: listExerciseTagUsage,
  })
}

export function useCreateExerciseTag() {
  const queryClient = useQueryClient()
  return useMutation<ExerciseTag, AxiosError<ApiError>, CreateExerciseTagRequest>({
    mutationFn: createExerciseTag,
    onSuccess: () => invalidateExerciseTagData(queryClient),
  })
}

export function useUpdateExerciseTag() {
  const queryClient = useQueryClient()
  return useMutation<ExerciseTag, AxiosError<ApiError>, { id: number; data: UpdateExerciseTagRequest }>({
    mutationFn: ({ id, data }) => updateExerciseTag(id, data),
    onSuccess: () => invalidateExerciseTagData(queryClient),
  })
}

export function useDeleteExerciseTag() {
  const queryClient = useQueryClient()
  return useMutation<void, AxiosError<ApiError>, number>({
    mutationFn: deleteExerciseTag,
    onSuccess: () => invalidateExerciseTagData(queryClient),
  })
}

export function useCreateExercise() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createExercise,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exerciseKeys.all }),
  })
}

export function useUpdateExercise(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: UpdateExerciseInput) => updateExercise(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: exerciseKeys.all })
      queryClient.invalidateQueries({ queryKey: exerciseKeys.detail(id) })
    },
  })
}

export function useDeactivateExercise() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deactivateExercise,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exerciseKeys.all }),
  })
}

export function useReactivateExercise() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: reactivateExercise,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: exerciseKeys.all }),
  })
}
