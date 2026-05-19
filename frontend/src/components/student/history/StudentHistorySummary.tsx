import { Activity, CalendarDays, Dumbbell, ListChecks } from "lucide-react"
import { Card, CardContent } from "@/components/ui/card"
import { formatDateEs } from "@/lib/date"
import type { StudentHistorySummary as StudentHistorySummaryType } from "@/types/studentHistory"

type StudentHistorySummaryProps = {
  summary?: StudentHistorySummaryType
  isLoading?: boolean
}

export function StudentHistorySummary({
  summary,
  isLoading,
}: StudentHistorySummaryProps) {
  if (isLoading) {
    return (
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }).map((_, index) => (
          <Card key={index} className="rounded-md">
            <CardContent className="p-5">
              <div className="h-4 w-28 animate-pulse rounded bg-muted" />
              <div className="mt-4 h-7 w-20 animate-pulse rounded bg-muted" />
              <div className="mt-3 h-4 w-36 animate-pulse rounded bg-muted" />
            </CardContent>
          </Card>
        ))}
      </div>
    )
  }

  if (!summary) return null

  const cards = [
    {
      title: "Rutinas totales",
      value: String(summary.totalRoutines),
      subtext:
        summary.totalRoutines === 1
          ? "1 rutina registrada"
          : summary.totalRoutines > 1
            ? `${summary.totalRoutines} rutinas registradas`
            : "Historial registrado",
      icon: ListChecks,
    },
    {
      title: "Activa actual",
      value: summary.activeRoutineName ?? "Sin rutina activa",
      subtext: summary.activeRoutineName
        ? activeDaysText(summary.activeRoutineDaysCount)
        : "No hay rutina activa actualmente",
      icon: Activity,
    },
    {
      title: "Ejercicios distintos",
      value: String(summary.distinctExercisesCount),
      subtext: "trabajados históricamente",
      icon: Dumbbell,
    },
    {
      title: "Entrenando desde",
      value: summary.trainingSince ? formatDateEs(summary.trainingSince) : "Sin rutinas",
      subtext: summary.trainingSince
        ? "primera rutina asignada"
        : "todavía no hay historial",
      icon: CalendarDays,
    },
  ]

  return (
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
      {cards.map((card) => {
        const Icon = card.icon
        return (
          <Card key={card.title} className="rounded-md">
            <CardContent className="p-5">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-sm font-medium text-muted-foreground">
                    {card.title}
                  </p>
                  <p className="mt-2 break-words text-2xl font-semibold tracking-normal text-slate-950">
                    {card.value}
                  </p>
                </div>
                <span className="rounded-md bg-emerald-50 p-2 text-emerald-700">
                  <Icon className="h-5 w-5" />
                </span>
              </div>
              <p className="mt-3 text-sm text-muted-foreground">{card.subtext}</p>
            </CardContent>
          </Card>
        )
      })}
    </div>
  )
}

function activeDaysText(days: number | null) {
  if (days === 0) return "asignada hoy"
  if (days === 1) return "hace 1 día"
  if (days && days > 1) return `hace ${days} días`
  return "fecha de asignación no definida"
}
