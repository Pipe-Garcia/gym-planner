import { ChevronDown } from "lucide-react"
import { Button } from "@/components/ui/button"
import { formatDateEs } from "@/lib/date"
import { structuralTypeLabel } from "@/lib/labels"
import { cn } from "@/lib/utils"
import type { StudentExerciseHistoryItem } from "@/types/studentHistory"
import { StudentExerciseOccurrencesList } from "./StudentExerciseOccurrencesList"

type StudentExerciseHistoryCardProps = {
  studentId: number
  exercise: StudentExerciseHistoryItem
  expanded: boolean
  onToggle: () => void
}

export function StudentExerciseHistoryCard({
  studentId,
  exercise,
  expanded,
  onToggle,
}: StudentExerciseHistoryCardProps) {
  return (
    <article
      className={cn(
        "relative overflow-hidden rounded-md border bg-white shadow-sm transition hover:border-emerald-200 hover:bg-emerald-50/20 hover:shadow-md",
        expanded &&
          "border-emerald-300 bg-emerald-50/40 shadow-md ring-1 ring-emerald-100 before:absolute before:left-0 before:top-0 before:h-full before:w-1 before:bg-emerald-500/60",
      )}
    >
      <div className="flex flex-col gap-3 p-4 sm:flex-row sm:items-start sm:justify-between">
        <button
          type="button"
          className="min-w-0 flex-1 rounded-md text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/30"
          onClick={onToggle}
          aria-expanded={expanded}
        >
          <div className="flex items-start gap-3">
            <ChevronDown
              className={cn(
                "mt-0.5 h-5 w-5 shrink-0 text-muted-foreground transition-transform",
                expanded && "rotate-180",
              )}
            />
            <div className="min-w-0">
              <h3 className="break-words text-[17px] font-semibold leading-6 tracking-normal text-slate-950">
                {exercise.exerciseName}
              </h3>
              <p className="mt-1.5 text-sm leading-5 text-slate-600">
                {lastOccurrenceText(exercise)} · {timesUsedText(exercise.timesUsed)}
              </p>
            </div>
          </div>
        </button>

        <Button
          type="button"
          variant="outline"
          size="sm"
          className={cn(
            "w-full sm:w-auto",
            expanded &&
              "border-emerald-300 bg-white text-emerald-700 shadow-sm hover:bg-emerald-50 hover:text-emerald-800",
          )}
          onClick={onToggle}
        >
          {expanded ? "Ocultar historial" : "Ver historial"}
        </Button>
      </div>

      {expanded ? (
        <div className="flex flex-wrap gap-2 px-4 pb-4 pl-12 sm:pl-12">
          {exercise.structuralTypesUsed.map((type) => (
            <span
              key={type}
              className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-700 ring-1 ring-slate-200"
            >
              {structuralTypeLabel(type)}
            </span>
          ))}
          {exercise.lastStructuralType ? (
            <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700 ring-1 ring-emerald-200">
              Última en {structuralTypeLabel(exercise.lastStructuralType)}
            </span>
          ) : null}
        </div>
      ) : null}

      {expanded ? (
        <div className="border-t border-emerald-100 bg-white/70 p-4">
          <StudentExerciseOccurrencesList
            studentId={studentId}
            exercise={exercise}
          />
        </div>
      ) : null}
    </article>
  )
}

function lastOccurrenceText(exercise: StudentExerciseHistoryItem) {
  const date = exercise.lastPerformedDate
    ? formatDateEs(exercise.lastPerformedDate)
    : "fecha no definida"
  const routine = exercise.lastRoutineName
  return routine ? `Última: ${date} · ${routine}` : `Última: ${date}`
}

function timesUsedText(timesUsed: number) {
  return timesUsed === 1 ? "trabajado 1 vez" : `trabajado ${timesUsed} veces`
}
