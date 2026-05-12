import { ClipboardList, Dumbbell, FileStack, UserRound } from "lucide-react"
import { Link } from "react-router-dom"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { useAuth } from "@/hooks/useAuth"

const quickLinks = [
  { label: "Alumnos", icon: UserRound, href: "/students", available: true },
  { label: "Ejercicios", icon: Dumbbell, href: "/exercises", available: true },
  { label: "Plantillas", icon: FileStack, href: "/templates", available: true },
  { label: "Rutinas", icon: ClipboardList, href: "/students", available: true, hint: "Las rutinas se gestionan desde la ficha del alumno." },
]

export function DashboardPage() {
  const { user } = useAuth()

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">Bienvenido, {user?.fullName}</h1>
        <p className="mt-1 text-sm text-muted-foreground">Modulos principales del panel interno.</p>
      </div>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {quickLinks.map((item) => {
          const Icon = item.icon
          return (
            <Card key={item.label} className={item.available ? "" : "opacity-70"}>
              <CardHeader className="pb-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-md bg-muted">
                  <Icon className="h-5 w-5 text-muted-foreground" />
                </div>
              </CardHeader>
              <CardContent>
                <CardTitle className="text-base">{item.label}</CardTitle>
                {"hint" in item ? <p className="mt-2 text-sm text-muted-foreground">{item.hint}</p> : null}
                {item.available ? (
                  <Link className="mt-2 inline-block text-sm font-medium text-primary" to={item.href}>
                    Abrir
                  </Link>
                ) : (
                  <p className="mt-2 text-sm text-muted-foreground">Próximamente</p>
                )}
              </CardContent>
            </Card>
          )
        })}
      </section>
    </div>
  )
}
