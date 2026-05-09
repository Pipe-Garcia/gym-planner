import { apiClient } from "@/api/client"
import type {
  CreateInjuryInput,
  CreateNoteInput,
  CreateStudentInput,
  Student,
  StudentInjury,
  StudentListParams,
  StudentNote,
  StudentPage,
  UpdateInjuryInput,
  UpdateStudentInput,
} from "@/types/student"

export async function listStudents(params: StudentListParams) {
  const response = await apiClient.get<StudentPage>("/api/students", { params })
  return response.data
}

export async function getStudent(id: number) {
  const response = await apiClient.get<Student>(`/api/students/${id}`)
  return response.data
}

export async function createStudent(data: CreateStudentInput) {
  const response = await apiClient.post<Student>("/api/students", data)
  return response.data
}

export async function updateStudent(id: number, data: UpdateStudentInput) {
  const response = await apiClient.put<Student>(`/api/students/${id}`, data)
  return response.data
}

export async function deactivateStudent(id: number) {
  await apiClient.delete(`/api/students/${id}`)
}

export async function reactivateStudent(id: number) {
  const response = await apiClient.patch<Student>(`/api/students/${id}/reactivate`)
  return response.data
}

export async function listInjuries(studentId: number, active?: boolean) {
  const response = await apiClient.get<StudentInjury[]>(`/api/students/${studentId}/injuries`, { params: { active } })
  return response.data
}

export async function createInjury(studentId: number, data: CreateInjuryInput) {
  const response = await apiClient.post<StudentInjury>(`/api/students/${studentId}/injuries`, data)
  return response.data
}

export async function updateInjury(studentId: number, injuryId: number, data: UpdateInjuryInput) {
  const response = await apiClient.put<StudentInjury>(`/api/students/${studentId}/injuries/${injuryId}`, data)
  return response.data
}

export async function deleteInjury(studentId: number, injuryId: number) {
  await apiClient.delete(`/api/students/${studentId}/injuries/${injuryId}`)
}

export async function listNotes(studentId: number) {
  const response = await apiClient.get<StudentNote[]>(`/api/students/${studentId}/notes`)
  return response.data
}

export async function createNote(studentId: number, data: CreateNoteInput) {
  const response = await apiClient.post<StudentNote>(`/api/students/${studentId}/notes`, data)
  return response.data
}

export async function deleteNote(studentId: number, noteId: number) {
  await apiClient.delete(`/api/students/${studentId}/notes/${noteId}`)
}
