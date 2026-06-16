import { apiClient } from "@/api/client"
import type {
  CreateExerciseInput,
  Exercise,
  ExerciseListParams,
  ExercisePage,
  ExerciseTag,
  ExerciseTagUsage,
  CreateExerciseTagRequest,
  TagType,
  UpdateExerciseInput,
  UpdateExerciseTagRequest,
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

export async function listExerciseTags(type?: TagType) {
  const response = await apiClient.get<ExerciseTag[]>("/api/exercise-tags", { params: { type } })
  return response.data
}

export const listTags = listExerciseTags

export async function listExerciseTagUsage() {
  const response = await apiClient.get<ExerciseTagUsage[]>("/api/exercise-tags/usage")
  return response.data
}

export async function createExerciseTag(data: CreateExerciseTagRequest) {
  const response = await apiClient.post<ExerciseTag>("/api/exercise-tags", data)
  return response.data
}

export async function updateExerciseTag(id: number, data: UpdateExerciseTagRequest) {
  const response = await apiClient.put<ExerciseTag>(`/api/exercise-tags/${id}`, data)
  return response.data
}

export async function deleteExerciseTag(id: number) {
  await apiClient.delete(`/api/exercise-tags/${id}`)
}
