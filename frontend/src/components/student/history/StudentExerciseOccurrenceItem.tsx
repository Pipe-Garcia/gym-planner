import { Eye } from "lucide-react"
import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { formatDateEs } from "@/lib/date"
import {
  purposeLabel,
  routineStatusBadgeClass,
  routineStatusLabel,
  structuralTypeLabel,
} from "@/lib/labels"
import type { StudentExerciseOccurrence } from "@/types/studentHistory"
import { StudentExerciseSetsSummary } from "./StudentExerciseSetsSummary"

type StudentExerciseOccurrenceItemProps = {
  studentId: number
  occurrence: StudentExerciseOccurrence
}

export function StudentExerciseOccurrenceItem({
  studentId,
  occurrence,
}: StudentExerciseOccurrenceItemProps) {
  return (
    <article className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0 space-y-2.5">
          <div className="flex flex-wrap items-center gap-2">
            <p className="break-words text-sm font-semibold leading-5 text-slate-950">
              {formatDateEs(occurrence.effectiveDate)} · {occurrence.routineName}
            </p>
            <span className={`${routineStatusBadgeClass(occurrence.routineStatus)} py-0.5`}>
              {routineStatusLabel(occurrence.routineStatus)}
            </span>
          </div>

          <p className="text-sm text-muted-foreground">
            {dayAndBlockText(occurrence)}
          </p>

          <div className="flex flex-wrap gap-2">
            <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700 ring-1 ring-emerald-200">
              {structuralTypeLabel(occurrence.blockStructuralType)}
            </span>
            {occurrence.blockPurpose ? (
              <span className="rounded-full bg-sky-50 px-2.5 py-1 text-xs font-medium text-sky-700 ring-1 ring-sky-200">
                {purposeLabel(occurrence.blockPurpose)}
              </span>
            ) : null}
          </div>
        </div>

        <Button variant="outline" size="sm" className="w-full sm:w-auto" asChild>
          <Link to={`/students/${studentId}/routines/${occurrence.routineId}`}>
            <Eye className="h-4 w-4" />
            Ver rutina
          </Link>
        </Button>
      </div>

      <div className="mt-4">
        <StudentExerciseSetsSummary
          sets={occurrence.sets}
          structuralType={occurrence.blockStructuralType}
        />
      </div>

      {occurrence.exerciseNotes?.trim() ? (
        <div className="mt-3 rounded-md border border-amber-200 bg-amber-50 px-3 py-2">
          <p className="text-xs font-semibold uppercase tracking-normal text-amber-800">
            Notas del ejercicio
          </p>
          <p className="mt-1 whitespace-pre-wrap text-sm text-amber-950">
            {occurrence.exerciseNotes}
          </p>
        </div>
      ) : null}
    </article>
  )
}

function dayAndBlockText(occurrence: StudentExerciseOccurrence) {
  const parts = [
    occurrence.dayName ??
      (occurrence.dayOrderIndex !== null
        ? `Día ${occurrence.dayOrderIndex}`
        : null),
    occurrence.blockTitle,
  ].filter(Boolean)

  return parts.length > 0 ? parts.join(" · ") : "Día o bloque no definido"
}
