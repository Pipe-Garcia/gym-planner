import { apiClient } from "@/api/client"
import type { LoginResponse } from "@/types/auth"

export async function loginRequest(email: string, password: string) {
  const { data } = await apiClient.post<LoginResponse>("/api/auth/login", { email, password })
  return data
}
