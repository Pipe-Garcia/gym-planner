export type UserRole = "OWNER" | "TRAINER"

export interface AuthenticatedUser {
  id: number
  email: string
  fullName: string
  role: UserRole
  gymId: number
}

export interface LoginResponse {
  token: string
  user: AuthenticatedUser
}
