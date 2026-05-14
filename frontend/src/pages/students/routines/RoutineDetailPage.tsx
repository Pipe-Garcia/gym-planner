import { Archive, CheckCircle, Copy, Download, MessageCircle, Pencil } from "lucide-react"
import { useState } from "react"
import { Link, useParams } from "react-router-dom"
import { downloadRoutinePdf, getRoutineWhatsAppText } from "@/api/routines"
import { DuplicateRoutineDialog } from "@/components/routine/DuplicateRoutineDialog"
import { BackButton } from "@/components/shared/BackButton"
import { TrainingPlanReadOnlyView } from "@/components/template/TrainingPlanReadOnlyView"
import { Button } from "@/components/ui/button"
import { useRoutine, useRoutineAction } from "@/hooks/useRoutines"
import { useToast } from "@/hooks/useToast"
import { formatDateEs } from "@/lib/date"
import { routineStatusBadgeClass, routineStatusLabel } from "@/lib/labels"

const actionButtonClass = "h-8 gap-1.5 px-2.5 text-xs"
const actionIconClass = "h-3.5 w-3.5"

export function RoutineDetailPage() {
  const studentId = Number(useParams().studentId)
  const routineId = Number(useParams().routineId)
  const toast = useToast()
  const [duplicateOpen, setDuplicateOpen] = useState(false)
  const routineQuery = useRoutine(routineId)
  const actions = useRoutineAction(studentId)
  const routine = routineQuery.data

  if (routineQuery.isLoading) return <div className="rounded-md border bg-white p-6 text-sm text-muted-foreground">Cargando rutina...</div>
  if (!routine) return <div className="rounded-md border bg-white p-6 text-sm text-muted-foreground">Rutina no encontrada.</div>

  async function finish() {
    await actions.finish.mutateAsync(routineId)
    toast.success("Rutina finalizada.")
    await routineQuery.refetch()
  }

  async function archive() {
    await actions.archive.mutateAsync(routineId)
    toast.success("Rutina archivada.")
    await routineQuery.refetch()
  }

  async function handleDownloadPdf() {
    try {
      const { blob, filename } = await downloadRoutinePdf(routineId)
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement("a")
      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      toast.success("PDF descargado.")
    } catch (error) {
      toast.error("No se pudo generar el PDF.")
      console.error("PDF download failed:", error)
    }
  }

  async function handleCopyWhatsApp() {
    try {
      const text = await getRoutineWhatsAppText(routineId)
      await navigator.clipboard.writeText(text)
      toast.success("Texto copiado al portapapeles.")
    } catch (error) {
      toast.error("No se pudo copiar. Probá generar de nuevo.")
      console.error("WhatsApp text copy failed:", error)
    }
  }

  return (
    <div className="space-y-6">
      <BackButton to={`/students/${routine.studentId}`} />
      <div className="flex flex-col gap-4 rounded-md border bg-white p-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="space-y-3">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold leading-tight tracking-normal">{routine.name}</h1>
            <span className={routineStatusBadgeClass(routine.status)}>{routineStatusLabel(routine.status)}</span>
          </div>
          <p className="text-base font-medium text-foreground">{routine.studentName}</p>
          <div className="grid gap-2 text-sm text-muted-foreground sm:grid-cols-3">
            <Meta label="Fecha asignada" value={formatDateEs(routine.assignedDate)} />
            <Meta label="Objetivo" value={routine.objective} />
            <Meta label="Plantilla origen" value={routine.sourceTemplateName || (routine.sourceTemplateId ? `Plantilla #${routine.sourceTemplateId}` : null)} />
          </div>
          {routine.generalNotes ? (
            <Note title="Para el alumno" value={routine.generalNotes} />
          ) : null}
          {routine.internalNotes ? (
            <Note title="Solo equipo" value={routine.internalNotes} privateNote />
          ) : null}
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Button type="button" size="sm" variant="outline" className={actionButtonClass} onClick={handleDownloadPdf}><Download className={actionIconClass} />PDF</Button>
          <Button type="button" size="sm" variant="outline" className={actionButtonClass} onClick={handleCopyWhatsApp}><MessageCircle className={actionIconClass} />WhatsApp</Button>
          {routine.status !== "FINISHED" && routine.status !== "ARCHIVED" ? (
            <Button asChild size="sm" className={actionButtonClass}>
              <Link to={`/students/${routine.studentId}/routines/${routine.id}/edit`}><Pencil className={actionIconClass} />Editar</Link>
            </Button>
          ) : null}
          <Button type="button" size="sm" variant="outline" className={actionButtonClass} onClick={() => setDuplicateOpen(true)}><Copy className={actionIconClass} />Duplicar</Button>
          {routine.status === "ACTIVE" ? (
            <Button type="button" size="sm" variant="outline" className={actionButtonClass} onClick={finish}><CheckCircle className={actionIconClass} />Finalizar</Button>
          ) : null}
          {routine.status === "ACTIVE" || routine.status === "FINISHED" ? (
            <Button type="button" size="sm" variant="outline" className={actionButtonClass} onClick={archive}><Archive className={actionIconClass} />Archivar</Button>
          ) : null}
        </div>
      </div>
      <TrainingPlanReadOnlyView days={routine.days} context="routine" />
      <DuplicateRoutineDialog routine={routine} open={duplicateOpen} onOpenChange={setDuplicateOpen} />
    </div>
  )
}

function Meta({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <p className="text-xs uppercase text-muted-foreground">{label}</p>
      <p className="text-sm text-foreground">{value || "-"}</p>
    </div>
  )
}

function Note({ title, value, privateNote = false }: { title: string; value: string; privateNote?: boolean }) {
  return (
    <div className={privateNote ? "max-w-3xl rounded-md border border-amber-200 bg-amber-50 p-3 text-sm" : "max-w-3xl rounded-md bg-muted/40 p-3 text-sm"}>
      <p className="font-medium">{title}</p>
      <p className="mt-1 text-muted-foreground">{value}</p>
    </div>
  )
}
