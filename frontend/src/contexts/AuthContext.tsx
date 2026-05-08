import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from "react"
import { loginRequest } from "@/api/auth"
import {
  clearAuthStorage,
  getStoredToken,
  getStoredUser,
  isTokenExpired,
  setAuthStorage,
} from "@/lib/auth-storage"
import type { AuthenticatedUser } from "@/types/auth"

interface AuthContextValue {
  user: AuthenticatedUser | null
  token: string | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const logout = useCallback(() => {
    clearAuthStorage()
    setUser(null)
    setToken(null)
  }, [])

  useEffect(() => {
    const storedToken = getStoredToken()
    const storedUser = getStoredUser()

    if (!storedToken || !storedUser || isTokenExpired(storedToken)) {
      logout()
      setIsLoading(false)
      return
    }

    setToken(storedToken)
    setUser(storedUser)
    setIsLoading(false)
  }, [logout])

  const login = useCallback(async (email: string, password: string) => {
    const response = await loginRequest(email, password)
    setAuthStorage(response.token, response.user)
    setToken(response.token)
    setUser(response.user)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(user && token),
      isLoading,
      login,
      logout,
    }),
    [isLoading, login, logout, token, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
