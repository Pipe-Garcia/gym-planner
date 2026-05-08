import type { AuthenticatedUser } from "@/types/auth"

const TOKEN_KEY = "gym_planner_token"
const USER_KEY = "gym_planner_user"

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as AuthenticatedUser
  } catch {
    clearAuthStorage()
    return null
  }
}

export function setAuthStorage(token: string, user: AuthenticatedUser) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearAuthStorage() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function isTokenExpired(token: string) {
  try {
    const payload = JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/"))) as {
      exp?: number
    }
    if (!payload.exp) {
      return true
    }
    return payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}
