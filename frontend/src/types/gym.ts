export interface Gym {
  id: number
  name: string
  ownerName?: string | null
  phone?: string | null
  email?: string | null
  address?: string | null
  logoUrl?: string | null
  primaryColor?: string | null
  createdAt: string
  updatedAt: string
}

export interface UpdateGymRequest {
  name: string
  ownerName?: string
  phone?: string
  email?: string
  address?: string
  primaryColor?: string
  logoUrl?: string
}
