import { LogOut, Menu } from "lucide-react"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/hooks/useAuth"

interface HeaderProps {
  gymName?: string
  onMenuClick: () => void
}

export function Header({ gymName, onMenuClick }: HeaderProps) {
  const { user, logout } = useAuth()

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b bg-white/95 px-4 backdrop-blur lg:px-6">
      <div className="flex min-w-0 items-center gap-3">
        <Button type="button" variant="ghost" size="icon" className="lg:hidden" onClick={onMenuClick} aria-label="Abrir menú">
          <Menu className="h-5 w-5" />
        </Button>
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold">{gymName ?? "Gym Planner"}</p>
          <p className="truncate text-xs text-muted-foreground">{user?.fullName}</p>
        </div>
      </div>
      <Button type="button" variant="outline" onClick={logout}>
        <LogOut className="h-4 w-4" />
        <span className="hidden sm:inline">Salir</span>
      </Button>
    </header>
  )
}
