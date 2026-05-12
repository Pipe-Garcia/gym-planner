import { Dumbbell, LayoutDashboard, Settings, UserRound, X, ClipboardList, FileStack } from "lucide-react"
import { NavLink } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface SidebarProps {
  open: boolean
  onClose: () => void
}

const navItems = [
  { label: "Dashboard", href: "/", icon: LayoutDashboard, disabled: false },
  { label: "Alumnos", href: "/students", icon: UserRound, disabled: false },
  { label: "Ejercicios", href: "/exercises", icon: Dumbbell, disabled: false },
  { label: "Plantillas", href: "/templates", icon: FileStack, disabled: false },
  { label: "Rutinas", href: "/routines", icon: ClipboardList, disabled: false },
  { label: "Configuración", href: "/settings", icon: Settings, disabled: false },
]

export function Sidebar({ open, onClose }: SidebarProps) {
  return (
    <>
      <div
        className={cn("fixed inset-0 z-30 bg-black/35 lg:hidden", open ? "block" : "hidden")}
        onClick={onClose}
        aria-hidden="true"
      />
      <aside
        className={cn(
          "fixed inset-y-0 left-0 z-40 flex w-72 flex-col border-r bg-white transition-transform lg:static lg:z-auto lg:translate-x-0",
          open ? "translate-x-0" : "-translate-x-full",
        )}
      >
        <div className="flex h-16 items-center justify-between border-b px-4">
          <div className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-md bg-primary text-primary-foreground">
              <Dumbbell className="h-5 w-5" />
            </span>
            <div>
              <p className="text-sm font-semibold">Gym Planner</p>
              <p className="text-xs text-muted-foreground">Panel interno</p>
            </div>
          </div>
          <Button type="button" variant="ghost" size="icon" className="lg:hidden" onClick={onClose} aria-label="Cerrar menú">
            <X className="h-5 w-5" />
          </Button>
        </div>

        <nav className="flex-1 space-y-1 p-3">
          {navItems.map((item) => {
            const Icon = item.icon
            if (item.disabled) {
              return (
                <button
                  key={item.label}
                  type="button"
                  disabled
                  className="flex min-h-11 w-full items-center gap-3 rounded-md px-3 text-left text-sm text-muted-foreground opacity-60"
                >
                  <Icon className="h-5 w-5" />
                  <span>{item.label}</span>
                </button>
              )
            }

            return (
              <NavLink
                key={item.label}
                to={item.href}
                onClick={onClose}
                className={({ isActive }) =>
                  cn(
                    "flex min-h-11 items-center gap-3 rounded-md px-3 text-sm font-medium transition-colors",
                    isActive ? "bg-primary text-primary-foreground" : "text-foreground hover:bg-muted",
                  )
                }
              >
                <Icon className="h-5 w-5" />
                <span>{item.label}</span>
              </NavLink>
            )
          })}
        </nav>
      </aside>
    </>
  )
}
