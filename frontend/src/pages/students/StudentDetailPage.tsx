import { Edit, RotateCcw, UserMinus } from "lucide-react"
import { useState } from "react"
import { Link, useParams } from "react-router-dom"
import { LoadingSpinner } from "@/components/shared/LoadingSpinner"
import { BackButton } from "@/components/shared/BackButton"
import { InjuryForm } from "@/components/student/InjuryForm"
import { InjuryList } from "@/components/student/InjuryList"
import { NoteForm } from "@/components/student/NoteForm"
import { NoteList } from "@/components/student/NoteList"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
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

const tabs = ["Datos", "Lesiones", "Notas", "Rutinas", "Historial"] as const
type Tab = (typeof tabs)[number]

export function StudentDetailPage() {
  const toast = useToast()
  const id = Number(useParams().id)
  const [tab, setTab] = useState<Tab>("Datos")
  const [injuryOpen, setInjuryOpen] = useState(false)
  const studentQuery = useStudent(id)
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
  if (!student) {
    return <p className="text-sm text-muted-foreground">Alumno no encontrado.</p>
  }

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
            <h1 className="text-2xl font-semibold tracking-normal">{student.firstName} {student.lastName}</h1>
            <span className={student.active ? "rounded-full bg-emerald-100 px-2 py-1 text-xs font-medium text-emerald-700" : "rounded-full bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600"}>
              {student.active ? "Activo" : "Inactivo"}
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">{student.documentId || "Sin DNI"}</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" asChild>
            <Link to={`/students/${student.id}/edit`}>
              <Edit className="h-4 w-4" />
              Editar
            </Link>
          </Button>
          <Button type="button" variant={student.active ? "destructive" : "outline"} onClick={toggleActive}>
            {student.active ? <UserMinus className="h-4 w-4" /> : <RotateCcw className="h-4 w-4" />}
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
              className={item === tab ? "border-b-2 border-primary px-3 py-3 text-sm font-semibold text-primary" : "px-3 py-3 text-sm text-muted-foreground"}
            >
              {item}
            </button>
          ))}
        </div>
      </div>

      {tab === "Datos" ? <StudentData student={student} /> : null}
      {tab === "Lesiones" ? (
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
      ) : null}
      {tab === "Notas" ? (
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
      ) : null}
      {tab === "Rutinas" || tab === "Historial" ? (
        <div className="rounded-md border bg-white p-8 text-center text-sm text-muted-foreground">Disponible próximamente</div>
      ) : null}
    </div>
  )
}

function StudentData({ student }: { student: NonNullable<ReturnType<typeof useStudent>["data"]> }) {
  const items = [
    ["Teléfono", student.phone],
    ["Email", student.email],
    ["Fecha de nacimiento", student.birthDate],
    ["Deporte", student.sport],
    ["Objetivo", student.objective],
    ["Nivel", student.level],
    ["Inicio en el gym", student.startedAt],
  ]

  return (
    <Card>
      <CardContent className="grid gap-4 p-6 sm:grid-cols-2">
        {items.map(([label, value]) => (
          <div key={label}>
            <p className="text-sm text-muted-foreground">{label}</p>
            <p className="mt-1 font-medium">{value || "-"}</p>
          </div>
        ))}
        <div className="sm:col-span-2">
          <p className="text-sm text-muted-foreground">Notas generales</p>
          <p className="mt-1 whitespace-pre-wrap text-sm">{student.generalNotes || "-"}</p>
        </div>
      </CardContent>
    </Card>
  )
}
