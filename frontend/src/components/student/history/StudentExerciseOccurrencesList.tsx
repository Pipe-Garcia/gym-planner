import { useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { useStudentExerciseOccurrences } from "@/hooks/useStudentHistory"
import type {
  StudentExerciseHistoryItem,
  StudentExerciseOccurrence,
} from "@/types/studentHistory"
import { StudentExerciseOccurrenceItem } from "./StudentExerciseOccurrenceItem"

const PAGE_SIZE = 10

type StudentExerciseOccurrencesListProps = {
  studentId: number
  exercise: StudentExerciseHistoryItem
}

export function StudentExerciseOccurrencesList({
  studentId,
  exercise,
}: StudentExerciseOccurrencesListProps) {
  const [page, setPage] = useState(0)
  const [items, setItems] = useState<StudentExerciseOccurrence[]>([])
  const occurrencesQuery = useStudentExerciseOccurrences({
    studentId,
    exerciseId: exercise.exerciseId,
    page,
    size: PAGE_SIZE,
    enabled: true,
  })

  useEffect(() => {
    setPage(0)
    setItems([])
  }, [studentId, exercise.exerciseId])

  useEffect(() => {
    if (!occurrencesQuery.data) return
    setItems((current) =>
      occurrencesQuery.data.page === 0
        ? occurrencesQuery.data.content
        : [...current, ...occurrencesQuery.data.content],
    )
  }, [occurrencesQuery.data])

  const isInitialLoading = occurrencesQuery.isLoading && items.length === 0
  const isLastPage = useMemo(() => {
    const data = occurrencesQuery.data
    if (!data) return true
    return data.last ?? data.page + 1 >= data.totalPages
  }, [occurrencesQuery.data])

  if (isInitialLoading) {
    return <OccurrencesSkeleton />
  }

  if (occurrencesQuery.isError && items.length === 0) {
    return (
      <OccurrenceError onRetry={() => void occurrencesQuery.refetch()} />
    )
  }

  if (items.length === 0) {
    return (
      <p className="rounded-md bg-slate-50 px-4 py-3 text-sm text-muted-foreground">
        No hay apariciones registradas para este ejercicio.
      </p>
    )
  }

  return (
    <div className="space-y-3.5">
      {items.map((occurrence, index) => (
        <StudentExerciseOccurrenceItem
          key={occurrenceKey(occurrence, index)}
          studentId={studentId}
          occurrence={occurrence}
        />
      ))}

      {occurrencesQuery.isError ? (
        <OccurrenceError
          compact
          onRetry={() => void occurrencesQuery.refetch()}
        />
      ) : null}

      {!isLastPage ? (
        <div className="flex justify-center">
          <Button
            type="button"
            variant="outline"
            onClick={() => setPage((current) => current + 1)}
            disabled={occurrencesQuery.isFetching}
          >
            {occurrencesQuery.isFetching
              ? "Cargando..."
              : "Mostrar más apariciones"}
          </Button>
        </div>
      ) : null}
    </div>
  )
}

function OccurrencesSkeleton() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 2 }).map((_, index) => (
        <div key={index} className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
          <div className="h-4 w-56 animate-pulse rounded bg-muted" />
          <div className="mt-3 h-4 w-72 animate-pulse rounded bg-muted" />
          <div className="mt-4 grid gap-2 sm:grid-cols-2">
            <div className="h-10 animate-pulse rounded bg-muted" />
            <div className="h-10 animate-pulse rounded bg-muted" />
          </div>
        </div>
      ))}
    </div>
  )
}

function OccurrenceError({
  compact = false,
  onRetry,
}: {
  compact?: boolean
  onRetry: () => void
}) {
  return (
    <div
      className={
        compact
          ? "rounded-md border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"
          : "rounded-md border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700"
      }
    >
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p>No se pudieron cargar las apariciones de este ejercicio.</p>
        <Button type="button" variant="outline" size="sm" onClick={onRetry}>
          Reintentar
        </Button>
      </div>
    </div>
  )
}

function occurrenceKey(
  occurrence: StudentExerciseOccurrence,
  index: number,
) {
  return [
    occurrence.routineId,
    occurrence.effectiveDate,
    occurrence.dayOrderIndex,
    occurrence.blockTitle,
    index,
  ].join("-")
}
