import { ClipboardList, Dumbbell, FileStack, UserRound } from "lucide-react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { useAuth } from "@/hooks/useAuth"

const quickLinks = [
  { label: "Alumnos", icon: UserRound },
  { label: "Ejercicios", icon: Dumbbell },
  { label: "Plantillas", icon: FileStack },
  { label: "Rutinas", icon: ClipboardList },
]

export function DashboardPage() {
  const { user } = useAuth()

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">Bienvenido, {user?.fullName}</h1>
        <p className="mt-1 text-sm text-muted-foreground">Módulos en desarrollo. Disponibles en próximas versiones.</p>
      </div>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {quickLinks.map((item) => {
          const Icon = item.icon
          return (
            <Card key={item.label} className="opacity-70">
              <CardHeader className="pb-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-md bg-muted">
                  <Icon className="h-5 w-5 text-muted-foreground" />
                </div>
              </CardHeader>
              <CardContent>
                <CardTitle className="text-base">{item.label}</CardTitle>
                <p className="mt-2 text-sm text-muted-foreground">Próximamente</p>
              </CardContent>
            </Card>
          )
        })}
      </section>
    </div>
  )
}
