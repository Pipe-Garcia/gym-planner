import { useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { useStudentHistoryTimeline } from "@/hooks/useStudentHistory"
import type { StudentRoutineTimelineItem as TimelineItem } from "@/types/studentHistory"
import { StudentHistoryEmptyState } from "./StudentHistoryEmptyState"
import { StudentHistoryTimelineItem } from "./StudentHistoryTimelineItem"

const PAGE_SIZE = 10

type StudentHistoryTimelineProps = {
  studentId: number
  enabled?: boolean
}

export function StudentHistoryTimeline({
  studentId,
  enabled = true,
}: StudentHistoryTimelineProps) {
  const [page, setPage] = useState(0)
  const [items, setItems] = useState<TimelineItem[]>([])
  const timelineQuery = useStudentHistoryTimeline({
    studentId,
    page,
    size: PAGE_SIZE,
    enabled,
  })

  useEffect(() => {
    setPage(0)
    setItems([])
  }, [studentId])

  useEffect(() => {
    if (!timelineQuery.data) return
    setItems((current) => {
      if (timelineQuery.data.page === 0) return timelineQuery.data.content
      const seen = new Set(current.map((item) => item.routineId))
      const next = timelineQuery.data.content.filter(
        (item) => !seen.has(item.routineId),
      )
      return [...current, ...next]
    })
  }, [timelineQuery.data])

  const isInitialLoading = timelineQuery.isLoading && items.length === 0
  const isLastPage = useMemo(() => {
    const data = timelineQuery.data
    if (!data) return true
    return data.last ?? data.page + 1 >= data.totalPages
  }, [timelineQuery.data])

  if (timelineQuery.isError && items.length === 0) {
    return <HistoryError />
  }

  if (isInitialLoading) {
    return <TimelineSkeleton />
  }

  if (items.length === 0) {
    return <StudentHistoryEmptyState />
  }

  return (
    <section className="space-y-4">
      <div>
        <h2 className="text-lg font-semibold tracking-normal">
          Línea de tiempo de ciclos
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Rutinas ordenadas de más reciente a más antigua.
        </p>
      </div>

      <div className="relative space-y-4 before:absolute before:bottom-0 before:left-[13px] before:top-2 before:w-px before:bg-border">
        {items.map((item) => (
          <StudentHistoryTimelineItem
            key={item.routineId}
            studentId={studentId}
            item={item}
          />
        ))}
      </div>

      {timelineQuery.isError ? <HistoryError compact /> : null}

      {!isLastPage ? (
        <div className="flex justify-center">
          <Button
            type="button"
            variant="outline"
            onClick={() => setPage((current) => current + 1)}
            disabled={timelineQuery.isFetching}
          >
            {timelineQuery.isFetching ? "Cargando..." : "Ver más"}
          </Button>
        </div>
      ) : null}
    </section>
  )
}

function TimelineSkeleton() {
  return (
    <section className="space-y-4">
      <div>
        <div className="h-6 w-56 animate-pulse rounded bg-muted" />
        <div className="mt-2 h-4 w-72 animate-pulse rounded bg-muted" />
      </div>
      <div className="space-y-4">
        {Array.from({ length: 3 }).map((_, index) => (
          <div key={index} className="pl-8">
            <div className="rounded-md border bg-white p-4">
              <div className="h-5 w-44 animate-pulse rounded bg-muted" />
              <div className="mt-3 h-4 w-64 animate-pulse rounded bg-muted" />
              <div className="mt-3 h-4 w-52 animate-pulse rounded bg-muted" />
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}

function HistoryError({ compact = false }: { compact?: boolean }) {
  return (
    <div
      className={
        compact
          ? "rounded-md border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700"
          : "rounded-md border border-rose-200 bg-rose-50 p-5 text-sm text-rose-700"
      }
    >
      No se pudo cargar el historial del alumno.
    </div>
  )
}
