import { Edit, Plus, RotateCcw, UserMinus } from "lucide-react"
import { useState } from "react"
import { Link, useParams } from "react-router-dom"
import { DuplicateRoutineDialog } from "@/components/routine/DuplicateRoutineDialog"
import { RoutineActionsBar } from "@/components/routine/RoutineActionsBar"
import { BackButton } from "@/components/shared/BackButton"
import { LoadingSpinner } from "@/components/shared/LoadingSpinner"
import { InjuryForm } from "@/components/student/InjuryForm"
import { InjuryList } from "@/components/student/InjuryList"
import { NoteForm } from "@/components/student/NoteForm"
import { NoteList } from "@/components/student/NoteList"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { useRoutine, useStudentRoutines } from "@/hooks/useRoutines"
import {
  useCreateInjury,
  useCreateNote,
  useDeactivateStudent,
  useDeleteInjury,
  useDeleteNote,
  useReactivateStudent,
  useStudent,
  useStudentInjuries,
  useStudentNotes,
} from "@/hooks/useStudents"
import { useToast } from "@/hooks/useToast"
import { formatDateEs } from "@/lib/date"
import { routineStatusBadgeClass, routineStatusLabel } from "@/lib/labels"
import type { RoutineSummary } from "@/types/training"

const tabs = ["Datos", "Lesiones", "Notas", "Rutinas", "Historial"] as const
type Tab = (typeof tabs)[number]

export function StudentDetailPage() {
  const toast = useToast()
  const id = Number(useParams().id)
  const [tab, setTab] = useState<Tab>("Datos")
  const [injuryOpen, setInjuryOpen] = useState(false)

  const studentQuery = useStudent(id)
  const routinesQuery = useStudentRoutines(id, {
    page: 0,
    size: 50,
    sort: "assignedDate,desc",
  })
  const injuriesQuery = useStudentInjuries(id, true)
  const notesQuery = useStudentNotes(id)
  const createInjury = useCreateInjury(id)
  const deleteInjury = useDeleteInjury(id)
  const createNote = useCreateNote(id)
  const deleteNote = useDeleteNote(id)
  const deactivate = useDeactivateStudent()
  const reactivate = useReactivateStudent()

  if (studentQuery.isLoading) {
    return (
      <div className="flex min-h-80 items-center justify-center">
        <LoadingSpinner />
      </div>
    )
  }

  const student = studentQuery.data
  if (!student)
    return (
      <p className="text-sm text-muted-foreground">Alumno no encontrado.</p>
    )

  async function toggleActive() {
    if (!student) return
    try {
      if (student.active) {
        await deactivate.mutateAsync(student.id)
        toast.success("Alumno desactivado.")
      } else {
        await reactivate.mutateAsync(student.id)
        toast.success("Alumno reactivado.")
      }
      await studentQuery.refetch()
    } catch {
      toast.error("No pudimos cambiar el estado.")
    }
  }

  return (
    <div className="space-y-6">
      <BackButton to="/students" />

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-normal">
              {student.firstName} {student.lastName}
            </h1>
            <span
              className={
                student.active
                  ? "rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-700"
                  : "rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600"
              }
            >
              {student.active ? "Activo" : "Inactivo"}
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">
            {student.documentId || "Sin DNI"}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" asChild>
            <Link to={`/students/${student.id}/edit`}>
              <Edit className="h-4 w-4" />
              Editar
            </Link>
          </Button>
          <Button
            type="button"
            variant={student.active ? "destructive" : "outline"}
            onClick={toggleActive}
          >
            {student.active ? (
              <UserMinus className="h-4 w-4" />
            ) : (
              <RotateCcw className="h-4 w-4" />
            )}
            {student.active ? "Desactivar" : "Reactivar"}
          </Button>
        </div>
      </div>

      <div className="overflow-x-auto">
        <div className="flex min-w-max gap-2 border-b">
          {tabs.map((item) => (
            <button
              key={item}
              type="button"
              onClick={() => setTab(item)}
              className={
                item === tab
                  ? "border-b-2 border-primary px-3 py-3 text-sm font-semibold text-primary"
                  : "px-3 py-3 text-sm text-muted-foreground"
              }
            >
              {item}
            </button>
          ))}
        </div>
      </div>

      {tab === "Datos" && <StudentData student={student} />}

      {tab === "Lesiones" && (
        <>
          <InjuryList
            injuries={injuriesQuery.data ?? []}
            onAdd={() => setInjuryOpen(true)}
            onResolve={async (injury) => {
              await deleteInjury.mutateAsync(injury.id)
              toast.success("Lesión resuelta.")
            }}
          />
          <InjuryForm
            open={injuryOpen}
            onOpenChange={setInjuryOpen}
            onSubmit={async (values) => {
              await createInjury.mutateAsync(values)
              toast.success("Lesión guardada.")
            }}
          />
        </>
      )}

      {tab === "Notas" && (
        <section className="space-y-4">
          <NoteForm
            onSubmit={async (values) => {
              await createNote.mutateAsync(values)
              toast.success("Nota agregada.")
            }}
          />
          <NoteList
            notes={notesQuery.data ?? []}
            onDelete={async (note) => {
              await deleteNote.mutateAsync(note.id)
              toast.success("Nota eliminada.")
            }}
          />
        </section>
      )}

      {tab === "Rutinas" && (
        <StudentRoutines
          studentId={student.id}
          routines={routinesQuery.data?.content ?? []}
        />
      )}

      {tab === "Historial" && (
        <div className="rounded-md border bg-white p-8 text-center text-sm text-muted-foreground">
          Disponible próximamente.
        </div>
      )}
    </div>
  )
}

function StudentRoutines({
  studentId,
  routines,
}: {
  studentId: number
  routines: RoutineSummary[]
}) {
  const active = routines.find((r) => r.status === "ACTIVE")
  const activeRoutineQuery = useRoutine(active?.id)
  const drafts = routines.filter((r) => r.status === "DRAFT")
  const history = routines.filter(
    (r) => r.status === "FINISHED" || r.status === "ARCHIVED"
  )

  return (
    <section className="space-y-4">
      {/* Rutina activa */}
      {active ? (
        <div className="rounded-md border border-emerald-300 bg-emerald-50/60 p-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <p className="font-semibold">{active.name}</p>
                <span
                  className={`${routineStatusBadgeClass(active.status)} py-0.5`}
                >
                  {routineStatusLabel(active.status)}
                </span>
              </div>
              <p className="text-sm text-muted-foreground">
                {formatDateEs(active.assignedDate)} · {active.dayCount} días ·{" "}
                {active.blockCount} bloques · {active.exerciseCount} ejercicios
              </p>
            </div>
            {activeRoutineQuery.data ? (
              <RoutineActionsBar
                routine={activeRoutineQuery.data}
                studentId={studentId}
                onRoutineChanged={() => void activeRoutineQuery.refetch()}
                compact
              />
            ) : (
              <div className="h-8 w-24 animate-pulse rounded-md bg-muted" />
            )}
          </div>
        </div>
      ) : (
        /* Sin rutina activa */
        <div className="flex flex-col gap-3 rounded-md border border-dashed bg-white p-5 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="font-medium">Sin rutina activa</p>
            <p className="text-sm text-muted-foreground">
              Creá una rutina nueva para empezar el próximo ciclo.
            </p>
          </div>
          <Button asChild>
            <Link to={`/students/${studentId}/routines/new`}>
              <Plus className="h-4 w-4" />
              Nueva rutina
            </Link>
          </Button>
        </div>
      )}

      {/* Borradores */}
      {drafts.length > 0 && (
        <div className="space-y-2">
          <h2 className="font-semibold">Borradores</h2>
          {drafts.map((r) => (
            <RoutineCard key={r.id} routine={r} />
          ))}
        </div>
      )}

      {/* Historial */}
      {history.length > 0 && (
        <div className="space-y-2">
          <h2 className="font-semibold">Anteriores</h2>
          {history.map((r) => (
            <RoutineCard key={r.id} routine={r} />
          ))}
        </div>
      )}
    </section>
  )
}

function RoutineCard({ routine }: { routine: RoutineSummary }) {
  const [duplicateOpen, setDuplicateOpen] = useState(false)
  const fullRoutineQuery = useRoutine(routine.id)

  return (
    <div className="rounded-md border bg-white p-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <p className="font-medium">{routine.name}</p>
            <span
              className={`${routineStatusBadgeClass(routine.status)} py-0.5`}
            >
              {routineStatusLabel(routine.status)}
            </span>
          </div>
          <p className="text-sm text-muted-foreground">
            {formatDateEs(routine.assignedDate)} · {routine.dayCount} días ·{" "}
            {routine.blockCount} bloques · {routine.exerciseCount} ejercicios
          </p>
        </div>

        {fullRoutineQuery.data ? (
          <RoutineActionsBar
            routine={fullRoutineQuery.data}
            studentId={routine.studentId}
            onRoutineChanged={() => void fullRoutineQuery.refetch()}
            compact
            onDuplicate={() => setDuplicateOpen(true)}
          />
        ) : (
          <div className="h-8 w-24 animate-pulse rounded-md bg-muted" />
        )}
      </div>
      <DuplicateRoutineDialog
        routine={routine}
        open={duplicateOpen}
        onOpenChange={setDuplicateOpen}
      />
    </div>
  )
}

function StudentData({
  student,
}: {
  student: NonNullable<ReturnType<typeof useStudent>["data"]>
}) {
  const items = [
    ["Teléfono", student.phone],
    ["Email", student.email],
    ["Fecha de nacimiento", formatDateEs(student.birthDate)],
    ["Deporte", student.sport],
    ["Objetivo", student.objective],
    ["Nivel", student.level],
    ["Inicio en el gym", formatDateEs(student.startedAt)],
  ]
  return (
    <Card>
      <CardContent className="grid gap-4 p-6 sm:grid-cols-2">
        {items.map(([label, value]) => (
          <div key={label}>
            <p className="text-sm text-muted-foreground">{label}</p>
            <p className="mt-1 font-medium">{value || "—"}</p>
          </div>
        ))}
        <div className="sm:col-span-2">
          <p className="text-sm text-muted-foreground">Notas generales</p>
          <p className="mt-1 whitespace-pre-wrap text-sm">
            {student.generalNotes || "—"}
          </p>
        </div>
      </CardContent>
    </Card>
  )
}