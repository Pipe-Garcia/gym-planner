import { apiClient } from "@/api/client"
import type {
  CreateExerciseInput,
  Exercise,
  ExerciseListParams,
  ExercisePage,
  ExerciseTag,
  TagType,
  UpdateExerciseInput,
} from "@/types/exercise"

export async function listExercises(params: ExerciseListParams) {
  const response = await apiClient.get<ExercisePage>("/api/exercises", {
    params,
    paramsSerializer: { indexes: null },
  })
  return response.data
}

export async function getExercise(id: number) {
  const response = await apiClient.get<Exercise>(`/api/exercises/${id}`)
  return response.data
}

export async function createExercise(data: CreateExerciseInput) {
  const response = await apiClient.post<Exercise>("/api/exercises", data)
  return response.data
}

export async function updateExercise(id: number, data: UpdateExerciseInput) {
  const response = await apiClient.put<Exercise>(`/api/exercises/${id}`, data)
  return response.data
}

export async function deactivateExercise(id: number) {
  await apiClient.delete(`/api/exercises/${id}`)
}

export async function reactivateExercise(id: number) {
  const response = await apiClient.patch<Exercise>(`/api/exercises/${id}/reactivate`)
  return response.data
}

export async function listTags(type?: TagType) {
  const response = await apiClient.get<ExerciseTag[]>("/api/exercise-tags", { params: { type } })
  return response.data
}
