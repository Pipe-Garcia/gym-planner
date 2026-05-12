import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createTemplate, deactivateTemplate, duplicateTemplate, getTemplate, listTemplates, reactivateTemplate, updateTemplate } from "@/api/templates"
import type { TemplateInput, TemplateListParams } from "@/types/training"

export function useTemplates(params: TemplateListParams) {
  return useQuery({ queryKey: ["templates", params], queryFn: () => listTemplates(params) })
}

export function useTemplate(id?: number) {
  return useQuery({ queryKey: ["templates", id], queryFn: () => getTemplate(id!), enabled: Boolean(id) })
}

export function useCreateTemplate() {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: createTemplate, onSuccess: () => queryClient.invalidateQueries({ queryKey: ["templates"] }) })
}

export function useUpdateTemplate(id: number) {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: (data: TemplateInput & { active?: boolean }) => updateTemplate(id, data), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["templates"] }) })
}

export function useDeactivateTemplate() {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: deactivateTemplate, onSuccess: () => queryClient.invalidateQueries({ queryKey: ["templates"] }) })
}

export function useReactivateTemplate() {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: reactivateTemplate, onSuccess: () => queryClient.invalidateQueries({ queryKey: ["templates"] }) })
}

export function useDuplicateTemplate() {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: duplicateTemplate, onSuccess: () => queryClient.invalidateQueries({ queryKey: ["templates"] }) })
}