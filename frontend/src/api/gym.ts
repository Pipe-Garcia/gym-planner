import { apiClient } from "@/api/client"
import type { Gym, UpdateGymRequest } from "@/types/gym"

export async function getCurrentGym() {
  const { data } = await apiClient.get<Gym>("/api/gym/current")
  return data
}

export async function updateCurrentGym(payload: UpdateGymRequest) {
  const { data } = await apiClient.put<Gym>("/api/gym/current", payload)
  return data
}
