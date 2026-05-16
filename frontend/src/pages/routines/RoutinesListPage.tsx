import { ClipboardList, Loader2, Plus } from "lucide-react"
import { useMemo, useState } from "react"
import { useNavigate, useSearchParams } from "react-router-dom"
import { DuplicateRoutineDialog } from "@/components/routine/DuplicateRoutineDialog"
import { RoutineActionsBar } from "@/components/routine/RoutineActionsBar"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { useRoutine, useRoutines } from "@/hooks/useRoutines"
import { useStudents } from "@/hooks/useStudents"
import { formatDateEs } from "@/lib/date"
import { routineStatusBadgeClass, routineStatusLabel } from "@/lib/labels"
import type { RoutineStatus, RoutineSummary } from "@/types/training"

const statusOptions: { value: "" | RoutineStatus; label: string }[] = [
  { value: "ACTIVE", label: "Activas" },
  { value: "DRAFT", label: "Borradores" },
  { value: "FINISHED", label: "Finalizadas" },
  { value: "ARCHIVED", label: "Archivadas" },
  { value: "", label: "Todos los estados" },
]

// Default sensato: al entrar a /routines mostrar solo las activas.
const DEFAULT_STATUS: RoutineStatus | "" = "ACTIVE"

export function RoutinesListPage() {
  const [searchParams, setSearchParams] = useSearchParams()

  // Estado derivado de la URL. Cada cambio se persiste en searchParams.
  const q = searchParams.get("q") ?? ""
  const status = (searchParams.get("status") ?? DEFAULT_STATUS) as RoutineStatus | ""
  const dateFrom = searchParams.get("dateFrom") ?? ""
  const dateTo = searchParams.get("dateTo") ?? ""
  const sport = searchParams.get("sport") ?? ""
  const level = searchParams.get("level") ?? ""

  const [newRoutineOpen, setNewRoutineOpen] = useState(false)
  const [duplicateRoutine, setDuplicateRoutine] = useState<RoutineSummary | null>(null)

  function updateParam(key: string, value: string) {
    const next = new URLSearchParams(searchParams)
    if (value) {
      next.set(key, value)
    } else {
      next.delete(key)
    }
    setSearchParams(next, { replace: true })
  }

  const query = useRoutines({
    q: q || undefined,
    status: status || undefined,
    dateFrom: dateFrom || undefined,
    dateTo: dateTo || undefined,
    sport: sport || undefined,
    level: level || undefined,
    page: 0,
    size: 50,
    sort: "assignedDate,desc",
  })
  const routines = query.data?.content ?? []

  // Para los selects de deporte y nivel, traemos opciones únicas
  // de los alumnos para no hardcodear listas estáticas.
  const studentsForOptions = useStudents({ active: true, page: 0, size: 200 })
  const sportOptions = useMemo(() => {
    const set = new Set<string>()
    studentsForOptions.data?.content.forEach((s) => {
      if (s.sport) set.add(s.sport)
    })
    return Array.from(set).sort()
  }, [studentsForOptions.data])
  const levelOptions = useMemo(() => {
    const set = new Set<string>()
    studentsForOptions.data?.content.forEach((s) => {
      if (s.level) set.add(s.level)
    })
    return Array.from(set).sort()
  }, [studentsForOptions.data])

  const hasActiveFilters =
    Boolean(q) ||
    status !== DEFAULT_STATUS ||
    Boolean(dateFrom) ||
    Boolean(dateTo) ||
    Boolean(sport) ||
    Boolean(level)

  function clearFilters() {
    setSearchParams({}, { replace: true })
  }

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-normal">Rutinas</h1>
          <p className="text-sm text-muted-foreground">
            Vista global de rutinas asignadas a alumnos.
          </p>
        </div>
        <Button onClick={() => setNewRoutineOpen(true)}>
          <Plus className="h-4 w-4" />
          Nueva rutina
        </Button>
      </div>

      <div className="space-y-3 rounded-md border bg-white p-4">
        <div className="grid gap-3 md:grid-cols-[minmax(0,1fr)_180px_160px_160px]">
          <Input
            value={q}
            onChange={(e) => updateParam("q", e.target.value)}
            placeholder="Buscar por alumno o rutina"
          />
          <select
            value={status}
            onChange={(e) => updateParam("status", e.target.value)}
            className="min-h-11 rounded-md border bg-white px-3 text-sm"
          >
            {statusOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <Input
            type="date"
            value={dateFrom}
            onChange={(e) => updateParam("dateFrom", e.target.value)}
          />
          <Input
            type="date"
            value={dateTo}
            onChange={(e) => updateParam("dateTo", e.target.value)}
          />
        </div>

        <div className="grid gap-3 md:grid-cols-[180px_180px_auto]">
          <select
            value={sport}
            onChange={(e) => updateParam("sport", e.target.value)}
            className="min-h-11 rounded-md border bg-white px-3 text-sm"
          >
            <option value="">Todos los deportes</option>
            {sportOptions.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
          <select
            value={level}
            onChange={(e) => updateParam("level", e.target.value)}
            className="min-h-11 rounded-md border bg-white px-3 text-sm"
          >
            <option value="">Todos los niveles</option>
            {levelOptions.map((l) => (
              <option key={l} value={l}>
                {l}
              </option>
            ))}
          </select>
          {hasActiveFilters && (
            <Button variant="ghost" size="sm" onClick={clearFilters} className="self-center justify-self-start">
              Limpiar filtros
            </Button>
          )}
        </div>
      </div>

      {routines.length ? (
        <div className="overflow-hidden rounded-md border bg-white">
          <div className="hidden grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)_130px_160px_120px_220px] border-b bg-muted/30 px-4 py-2 text-xs font-medium uppercase text-muted-foreground lg:grid">
            <span>Rutina</span>
            <span>Alumno</span>
            <span>Estado</span>
            <span>Plantilla origen</span>
            <span>Fecha</span>
            <span className="text-right">Acciones</span>
          </div>
          <div className="divide-y">
            {routines.map((routine) => (
              <RoutineRow
                key={routine.id}
                routine={routine}
                onDuplicate={() => setDuplicateRoutine(routine)}
              />
            ))}
          </div>
        </div>
      ) : (
        <div className="rounded-md border bg-white p-8 text-center text-sm text-muted-foreground">
          <ClipboardList className="mx-auto mb-2 h-8 w-8" />
          No hay rutinas para los filtros seleccionados.
        </div>
      )}

      <NewRoutineDialog open={newRoutineOpen} onOpenChange={setNewRoutineOpen} />
      <DuplicateRoutineDialog
        routine={duplicateRoutine}
        open={Boolean(duplicateRoutine)}
        onOpenChange={(open) => !open && setDuplicateRoutine(null)}
      />
    </div>
  )
}

function RoutineRow({
  routine,
  onDuplicate,
}: {
  routine: RoutineSummary
  onDuplicate: () => void
}) {
  const fullRoutineQuery = useRoutine(routine.id)

  return (
    <div className="grid gap-3 px-4 py-3 lg:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)_130px_160px_120px_220px] lg:items-center">
      <div>
        <p className="font-medium">{routine.name}</p>
        <p className="text-xs text-muted-foreground">
          {routine.dayCount} días · {routine.blockCount} bloques · {routine.exerciseCount} ejercicios
        </p>
      </div>

      <div className="text-sm">{routine.studentName}</div>

      <div>
        <span className={routineStatusBadgeClass(routine.status)}>
          {routineStatusLabel(routine.status)}
        </span>
      </div>

      <div className="text-sm text-muted-foreground">
        {routine.sourceTemplateName || (routine.sourceTemplateId ? `#${routine.sourceTemplateId}` : "—")}
      </div>

      <div className="text-sm text-muted-foreground">{formatDateEs(routine.assignedDate)}</div>

      <div className="flex justify-end">
        {fullRoutineQuery.data ? (
          <RoutineActionsBar
            routine={fullRoutineQuery.data}
            studentId={routine.studentId}
            onRoutineChanged={() => void fullRoutineQuery.refetch()}
            compact
            onDuplicate={onDuplicate}
          />
        ) : (
          <div className="h-8 w-24 animate-pulse rounded-md bg-muted" />
        )}
      </div>
    </div>
  )
}

function NewRoutineDialog({
  open,
  onOpenChange,
}: {
  open: boolean
  onOpenChange: (v: boolean) => void
}) {
  const navigate = useNavigate()
  const [search, setSearch] = useState("")

  const studentsQuery = useStudents({
    search: search || undefined,
    active: true,
    page: 0,
    size: 20,
  })
  const students = studentsQuery.data?.content ?? []

  function handleSelect(studentId: number) {
    onOpenChange(false)
    setSearch("")
    navigate(`/students/${studentId}/routines/new`)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Nueva rutina</DialogTitle>
          <DialogDescription>
            Seleccioná el alumno para crear la rutina.
          </DialogDescription>
        </DialogHeader>

        <Input
          placeholder="Buscar alumno..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          autoFocus
        />

        <div className="max-h-72 divide-y overflow-y-auto rounded-md border">
          {studentsQuery.isLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
            </div>
          ) : students.length === 0 ? (
            <p className="px-4 py-4 text-sm text-muted-foreground">
              No se encontraron alumnos.
            </p>
          ) : (
            students.map((student) => (
              <button
                key={student.id}
                type="button"
                className="w-full px-4 py-3 text-left text-sm transition-colors hover:bg-muted/50"
                onClick={() => handleSelect(student.id)}
              >
                <span className="font-medium">
                  {student.firstName} {student.lastName}
                </span>
                {student.sport && (
                  <span className="ml-2 text-xs text-muted-foreground">
                    {student.sport}
                  </span>
                )}
              </button>
            ))
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}