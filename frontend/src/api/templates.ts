import { apiClient } from "@/api/client"
import type { Template, TemplateInput, TemplateListParams, TemplatePage } from "@/types/training"

export async function listTemplates(params: TemplateListParams) {
  const response = await apiClient.get<TemplatePage>("/api/templates", { params })
  return response.data
}

export async function getTemplate(id: number) {
  const response = await apiClient.get<Template>(`/api/templates/${id}`)
  return response.data
}

export async function createTemplate(data: TemplateInput) {
  const response = await apiClient.post<Template>("/api/templates", data)
  return response.data
}

export async function updateTemplate(id: number, data: TemplateInput & { active?: boolean }) {
  const response = await apiClient.put<Template>(`/api/templates/${id}`, data)
  return response.data
}

export async function deactivateTemplate(id: number) {
  await apiClient.delete(`/api/templates/${id}`)
}

export async function reactivateTemplate(id: number) {
  const response = await apiClient.post<Template>(`/api/templates/${id}/reactivate`)
  return response.data
}

export async function duplicateTemplate(id: number) {
  const response = await apiClient.post<Template>(`/api/templates/${id}/duplicate`)
  return response.data
}