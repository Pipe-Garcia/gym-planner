import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  createInjury,
  createNote,
  createStudent,
  deactivateStudent,
  deleteInjury,
  deleteNote,
  getStudent,
  listInjuries,
  listNotes,
  listStudents,
  reactivateStudent,
  updateInjury,
  updateStudent,
} from "@/api/students"
import type { CreateInjuryInput, CreateNoteInput, StudentListParams, UpdateInjuryInput, UpdateStudentInput } from "@/types/student"

export function useStudents(params: StudentListParams) {
  return useQuery({
    queryKey: ["students", params],
    queryFn: () => listStudents(params),
  })
}

export function useStudent(id?: number) {
  return useQuery({
    queryKey: ["students", id],
    queryFn: () => getStudent(id!),
    enabled: Boolean(id),
  })
}

export function useCreateStudent() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createStudent,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["students"] }),
  })
}

export function useUpdateStudent(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: UpdateStudentInput) => updateStudent(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["students"] })
      queryClient.invalidateQueries({ queryKey: ["students", id] })
    },
  })
}

export function useDeactivateStudent() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deactivateStudent,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["students"] }),
  })
}

export function useReactivateStudent() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: reactivateStudent,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["students"] }),
  })
}

export function useStudentInjuries(studentId?: number, active = true) {
  return useQuery({
    queryKey: ["students", studentId, "injuries", active],
    queryFn: () => listInjuries(studentId!, active),
    enabled: Boolean(studentId),
  })
}

export function useCreateInjury(studentId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateInjuryInput) => createInjury(studentId, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["students", studentId] }),
  })
}

export function useUpdateInjury(studentId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ injuryId, data }: { injuryId: number; data: UpdateInjuryInput }) => updateInjury(studentId, injuryId, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["students", studentId] }),
  })
}

export function useDeleteInjury(studentId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (injuryId: number) => deleteInjury(studentId, injuryId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["students", studentId] }),
  })
}

export function useStudentNotes(studentId?: number) {
  return useQuery({
    queryKey: ["students", studentId, "notes"],
    queryFn: () => listNotes(studentId!),
    enabled: Boolean(studentId),
  })
}

export function useCreateNote(studentId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateNoteInput) => createNote(studentId, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["students", studentId, "notes"] }),
  })
}

export function useDeleteNote(studentId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (noteId: number) => deleteNote(studentId, noteId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["students", studentId, "notes"] }),
  })
}
