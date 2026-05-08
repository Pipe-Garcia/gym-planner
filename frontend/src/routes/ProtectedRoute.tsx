import { Navigate, Outlet } from "react-router-dom"
import { LoadingSpinner } from "@/components/shared/LoadingSpinner"
import { useAuth } from "@/hooks/useAuth"

export function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <LoadingSpinner />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return <Outlet />
}
