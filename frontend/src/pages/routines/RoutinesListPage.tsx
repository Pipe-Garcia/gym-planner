import { ClipboardList, Copy, Eye, Pencil } from "lucide-react"
import { useState } from "react"
import { Link } from "react-router-dom"
import { DuplicateRoutineDialog } from "@/components/routine/DuplicateRoutineDialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useRoutines } from "@/hooks/useRoutines"
import { formatDateEs } from "@/lib/date"
import { routineStatusBadgeClass, routineStatusLabel } from "@/lib/labels"
import type { RoutineStatus, RoutineSummary } from "@/types/training"

const actionButtonClass = "h-8 gap-1.5 px-2.5 text-xs"
const actionIconClass = "h-3.5 w-3.5"

const statusOptions: { value: "" | RoutineStatus; label: string }[] = [
  { value: "", label: "Todos los estados" },
  { value: "ACTIVE", label: "Activas" },
  { value: "DRAFT", label: "Borradores" },
  { value: "FINISHED", label: "Finalizadas" },
  { value: "ARCHIVED", label: "Archivadas" },
]

export function RoutinesListPage() {
  const [q, setQ] = useState("")
  const [status, setStatus] = useState("")
  const [dateFrom, setDateFrom] = useState("")
  const [dateTo, setDateTo] = useState("")
  const [duplicateRoutine, setDuplicateRoutine] = useState<RoutineSummary | null>(null)
  const query = useRoutines({
    q: q || undefined,
    status: status || undefined,
    dateFrom: dateFrom || undefined,
    dateTo: dateTo || undefined,
    page: 0,
    size: 50,
    sort: "assignedDate,desc",
  })
  const routines = query.data?.content ?? []

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">Rutinas</h1>
        <p className="text-sm text-muted-foreground">Vista global de rutinas asignadas a alumnos.</p>
      </div>

      <div className="grid gap-3 rounded-md border bg-white p-4 md:grid-cols-[minmax(0,1fr)_180px_160px_160px]">
        <Input value={q} onChange={(event) => setQ(event.target.value)} placeholder="Buscar por alumno o rutina" />
        <select value={status} onChange={(event) => setStatus(event.target.value)} className="min-h-11 rounded-md border bg-white px-3 text-sm">
          {statusOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
        </select>
        <Input type="date" value={dateFrom} onChange={(event) => setDateFrom(event.target.value)} />
        <Input type="date" value={dateTo} onChange={(event) => setDateTo(event.target.value)} />
      </div>

      {routines.length ? (
        <div className="overflow-hidden rounded-md border bg-white">
          <div className="hidden grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)_130px_160px_120px_220px] border-b bg-muted/30 px-4 py-2 text-xs font-medium uppercase text-muted-foreground lg:grid">
            <span>Rutina</span>
            <span>Alumno</span>
            <span>Estado</span>
            <span>Plantilla origen</span>
            <span>Fecha</span>
            <span>Acciones</span>
          </div>
          <div className="divide-y">
            {routines.map((routine) => (
              <div key={routine.id} className="grid gap-3 px-4 py-3 lg:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)_130px_160px_120px_220px] lg:items-center">
                <div>
                  <p className="font-medium">{routine.name}</p>
                  <p className="text-xs text-muted-foreground">{routine.dayCount} dias · {routine.blockCount} bloques · {routine.exerciseCount} ejercicios</p>
                </div>
                <div className="text-sm">{routine.studentName}</div>
                <div style={{ justifyContent: "center", display: "flex" }}><span className={routineStatusBadgeClass(routine.status)}>{routineStatusLabel(routine.status)}</span></div>
                <div className="text-sm text-muted-foreground">{routine.sourceTemplateName || (routine.sourceTemplateId ? `#${routine.sourceTemplateId}` : "-")}</div>
                <div className="text-sm text-muted-foreground">{formatDateEs(routine.assignedDate)}</div>
                <div className="flex items-center justify-start gap-2 whitespace-nowrap lg:justify-end">
                  <Button asChild size="sm" variant="outline" className={actionButtonClass}>
                    <Link to={`/students/${routine.studentId}/routines/${routine.id}`}><Eye className={actionIconClass} />Ver</Link>
                  </Button>
                  {routine.status !== "FINISHED" && routine.status !== "ARCHIVED" ? (
                    <Button asChild size="sm" variant="outline" className={actionButtonClass}>
                      <Link to={`/students/${routine.studentId}/routines/${routine.id}/edit`}><Pencil className={actionIconClass} />Editar</Link>
                    </Button>
                  ) : null}
                  <Button type="button" size="sm" variant="outline" className={actionButtonClass} onClick={() => setDuplicateRoutine(routine)}>
                    <Copy className={actionIconClass} />Duplicar
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : (
        <div className="rounded-md border bg-white p-8 text-center text-sm text-muted-foreground">
          <ClipboardList className="mx-auto mb-2 h-8 w-8" />
          No hay rutinas para los filtros seleccionados.
        </div>
      )}
      <DuplicateRoutineDialog routine={duplicateRoutine} open={Boolean(duplicateRoutine)} onOpenChange={(open) => !open && setDuplicateRoutine(null)} />
    </div>
  )
}
