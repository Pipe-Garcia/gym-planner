import { Eye } from "lucide-react"
import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { formatDateEs } from "@/lib/date"
import { routineStatusBadgeClass, routineStatusLabel } from "@/lib/labels"
import { cn } from "@/lib/utils"
import type { RoutineStatus } from "@/types/training"
import type { StudentRoutineTimelineItem as TimelineItem } from "@/types/studentHistory"

type StudentHistoryTimelineItemProps = {
  studentId: number
  item: TimelineItem
  isLast: boolean
}

export function StudentHistoryTimelineItem({
  studentId,
  item,
  isLast,
}: StudentHistoryTimelineItemProps) {
  return (
    <div className="relative z-10 pl-8">
      {isLast ? (
        <span className="absolute bottom-0 left-0 top-[31px] z-[1] w-6 bg-background" />
      ) : null}
      <span
        className={cn(
          "absolute left-[5px] top-6 z-10 h-3 w-3 rounded-full ring-4 ring-white",
          statusDotClass(item.status),
        )}
      />
      <div className="relative z-10 rounded-md border bg-white p-4 shadow-sm">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0 space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="break-words text-base font-semibold tracking-normal">
                {item.routineName}
              </h3>
              <span className={`${routineStatusBadgeClass(item.status)} py-0.5`}>
                {routineStatusLabel(item.status)}
              </span>
            </div>
            <div className="flex flex-wrap gap-x-3 gap-y-1 text-sm text-muted-foreground">
              <span>{dateRangeText(item)}</span>
              <span>Duración: {durationText(item)}</span>
            </div>
            <p className="text-sm text-slate-700">
              {countText(item.daysCount, "día", "días")} ·{" "}
              {countText(item.blocksCount, "bloque", "bloques")} ·{" "}
              {countText(item.exercisesCount, "ejercicio", "ejercicios")}
            </p>
            {item.sourceTemplateName ? (
              <p className="text-sm text-muted-foreground">
                Plantilla: {item.sourceTemplateName}
              </p>
            ) : null}
          </div>

          <Button variant="outline" size="sm" asChild>
            <Link to={`/students/${studentId}/routines/${item.routineId}`}>
              <Eye className="h-4 w-4" />
              Ver
            </Link>
          </Button>
        </div>

        {item.closureNotes?.trim() ? (
          <div className="mt-4 rounded-md border border-amber-200 bg-amber-50 p-3">
            <p className="text-xs font-semibold uppercase tracking-normal text-amber-800">
              Cierre
            </p>
            <p className="mt-1 whitespace-pre-wrap text-sm text-amber-950">
              {item.closureNotes}
            </p>
          </div>
        ) : null}
      </div>
    </div>
  )
}

function dateRangeText(item: TimelineItem) {
  if (item.assignedDate && item.finishedDate) {
    return `${formatDateEs(item.assignedDate)} - ${formatDateEs(item.finishedDate)}`
  }
  if (item.assignedDate && item.status === "ACTIVE") {
    return `Desde ${formatDateEs(item.assignedDate)} · actualmente activa`
  }
  if (item.assignedDate) {
    return `Desde ${formatDateEs(item.assignedDate)}`
  }
  return "Fecha no definida"
}

function durationText(item: TimelineItem) {
  const durationDays = item.durationDays
  if (durationDays === null) return "duración no definida"
  if (durationDays === 0) {
    return item.status === "ACTIVE" ? "asignada hoy" : "mismo día"
  }
  if (durationDays === 1) return "1 día"
  return `${durationDays} días`
}

function countText(count: number, singular: string, plural: string) {
  return `${count} ${count === 1 ? singular : plural}`
}

function statusDotClass(status: RoutineStatus) {
  const classes: Record<RoutineStatus, string> = {
    ACTIVE: "bg-emerald-500",
    FINISHED: "bg-sky-500",
    ARCHIVED: "bg-slate-400",
    DRAFT: "bg-amber-400",
  }
  return classes[status]
}
