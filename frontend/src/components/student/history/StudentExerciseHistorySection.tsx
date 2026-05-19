import { Search } from "lucide-react"
import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useStudentExerciseHistory } from "@/hooks/useStudentHistory"
import type { StudentExerciseHistoryItem } from "@/types/studentHistory"
import { StudentExerciseHistoryCard } from "./StudentExerciseHistoryCard"

const PAGE_SIZE = 20
const SEARCH_DEBOUNCE_MS = 300

type StudentExerciseHistorySectionProps = {
  studentId: number
}

export function StudentExerciseHistorySection({
  studentId,
}: StudentExerciseHistorySectionProps) {
  const sectionRef = useRef<HTMLElement | null>(null)
  const [search, setSearch] = useState("")
  const [debouncedSearch, setDebouncedSearch] = useState("")
  const [page, setPage] = useState(0)
  const [items, setItems] = useState<StudentExerciseHistoryItem[]>([])
  const [expandedIds, setExpandedIds] = useState<Set<number>>(() => new Set())
  const [isSearchFocused, setIsSearchFocused] = useState(false)

  const focusSection = useCallback(() => {
    sectionRef.current?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    })
  }, [])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      setDebouncedSearch(search.trim())
    }, SEARCH_DEBOUNCE_MS)

    return () => window.clearTimeout(timeoutId)
  }, [search])

  useEffect(() => {
    setPage(0)
    setItems([])
    setExpandedIds(new Set())
  }, [studentId, debouncedSearch])

  useEffect(() => {
    if (isSearchFocused) {
      focusSection()
    }
  }, [debouncedSearch, focusSection, isSearchFocused])

  const exerciseHistoryQuery = useStudentExerciseHistory({
    studentId,
    search: debouncedSearch,
    page,
    size: PAGE_SIZE,
  })

  useEffect(() => {
    if (!exerciseHistoryQuery.data) return
    setItems((current) => {
      if (exerciseHistoryQuery.data.page === 0) {
        return exerciseHistoryQuery.data.content
      }
      const seen = new Set(current.map((item) => item.exerciseId))
      const next = exerciseHistoryQuery.data.content.filter(
        (item) => !seen.has(item.exerciseId),
      )
      return [...current, ...next]
    })
  }, [exerciseHistoryQuery.data])

  const isInitialLoading = exerciseHistoryQuery.isLoading && items.length === 0
  const isLastPage = useMemo(() => {
    const data = exerciseHistoryQuery.data
    if (!data) return true
    return data.last ?? data.page + 1 >= data.totalPages
  }, [exerciseHistoryQuery.data])

  function toggleExpanded(exerciseId: number) {
    setExpandedIds((current) => {
      const next = new Set(current)
      if (next.has(exerciseId)) {
        next.delete(exerciseId)
      } else {
        next.add(exerciseId)
      }
      return next
    })
  }

  return (
    <section
      ref={sectionRef}
      className="scroll-mt-24 rounded-2xl border border-emerald-100 bg-white p-5 shadow-sm transition focus-within:border-emerald-300 focus-within:shadow-md focus-within:ring-1 focus-within:ring-emerald-100 sm:p-6"
    >
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 className="text-lg font-semibold tracking-normal">
            Historial por ejercicio
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Buscá un ejercicio para revisar cuándo y cómo fue trabajado.
          </p>
        </div>
        <div className="relative w-full lg:max-w-md">
          <Search className="pointer-events-none absolute left-3 top-3.5 h-5 w-5 text-emerald-700" />
          <Input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            onFocus={() => {
              setIsSearchFocused(true)
              focusSection()
            }}
            onBlur={() => setIsSearchFocused(false)}
            placeholder="Buscar por nombre de ejercicio..."
            className="h-12 border-slate-300 bg-white pl-10 shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500/30"
          />
        </div>
      </div>

      {exerciseHistoryQuery.isFetching && items.length > 0 ? (
        <p className="mt-4 text-xs text-muted-foreground">Actualizando historial...</p>
      ) : null}

      <div className="mt-5">
        {exerciseHistoryQuery.isError && items.length === 0 ? (
          <ExerciseHistoryError
            onRetry={() => void exerciseHistoryQuery.refetch()}
          />
        ) : isInitialLoading ? (
          <ExerciseHistorySkeleton />
        ) : items.length === 0 ? (
          <ExerciseHistoryEmpty search={debouncedSearch} />
        ) : (
          <div className="space-y-3.5">
            {items.map((exercise) => (
              <StudentExerciseHistoryCard
                key={exercise.exerciseId}
                studentId={studentId}
                exercise={exercise}
                expanded={expandedIds.has(exercise.exerciseId)}
                onToggle={() => toggleExpanded(exercise.exerciseId)}
              />
            ))}
          </div>
        )}
      </div>

      {exerciseHistoryQuery.isError && items.length > 0 ? (
        <ExerciseHistoryError
          compact
          onRetry={() => void exerciseHistoryQuery.refetch()}
        />
      ) : null}

      {items.length > 0 && !isLastPage ? (
        <div className="mt-5 flex justify-center">
          <Button
            type="button"
            variant="outline"
            onClick={() => setPage((current) => current + 1)}
            disabled={exerciseHistoryQuery.isFetching}
          >
            {exerciseHistoryQuery.isFetching
              ? "Cargando..."
              : "Mostrar más ejercicios"}
          </Button>
        </div>
      ) : null}
    </section>
  )
}

function ExerciseHistorySkeleton() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 4 }).map((_, index) => (
        <div key={index} className="rounded-md border border-slate-200 bg-white p-4 shadow-sm">
          <div className="h-5 w-56 animate-pulse rounded bg-muted" />
          <div className="mt-3 h-4 w-80 max-w-full animate-pulse rounded bg-muted" />
          <div className="mt-4 flex gap-2">
            <div className="h-7 w-20 animate-pulse rounded-full bg-muted" />
            <div className="h-7 w-24 animate-pulse rounded-full bg-muted" />
          </div>
        </div>
      ))}
    </div>
  )
}

function ExerciseHistoryEmpty({ search }: { search: string }) {
  if (search) {
    return (
      <div className="rounded-md border border-dashed bg-white p-5 text-sm text-muted-foreground">
        No se encontraron ejercicios para “{search}”.
      </div>
    )
  }

  return (
    <div className="rounded-md border border-dashed bg-white p-5">
      <p className="text-sm font-medium text-slate-900">
        Este alumno todavía no tiene ejercicios registrados en rutinas.
      </p>
      <p className="mt-1 text-sm text-muted-foreground">
        Cuando tenga rutinas asignadas, aparecerán acá.
      </p>
    </div>
  )
}

function ExerciseHistoryError({
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
          : "rounded-md border border-rose-200 bg-rose-50 p-5 text-sm text-rose-700"
      }
    >
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p>No se pudo cargar el historial por ejercicio.</p>
        <Button type="button" variant="outline" size="sm" onClick={onRetry}>
          Reintentar
        </Button>
      </div>
    </div>
  )
}
