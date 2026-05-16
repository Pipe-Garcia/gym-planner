import {
  Archive,
  CheckCircle,
  Download,
  Loader2,
  MessageCircle,
  MoreHorizontal,
  Plus,
  Trash2,
} from "lucide-react"
import { useState } from "react"
import { Link, useNavigate } from "react-router-dom"
import { downloadRoutinePdf, getRoutineWhatsAppText } from "@/api/routines"
import { CreateNextDialog } from "@/components/routine/CreateNextDialog"
import { FinishAndCreateNextDialog } from "@/components/routine/FinishAndCreateNextDialog"
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Textarea } from "@/components/ui/textarea"
import {
  useActivateRoutine,
  useArchiveRoutine,
  useDeleteRoutine,
  useFinishRoutine,
} from "@/hooks/useRoutines"
import { useToast } from "@/hooks/useToast"
import type { RoutineResponse } from "@/types/training"

interface RoutineActionsBarProps {
  routine: RoutineResponse
  studentId: number
  onRoutineChanged: () => void
  /** "view" = página de presentación, "editor" = página de edición. Solo aplica en modo no-compact. */
  mode?: "view" | "editor"
  /** En listas: un botón primario + un dropdown "•••". En páginas de detalle: botones expandidos. */
  compact?: boolean
  /** Callback para "Duplicar". Si no se pasa, la opción no aparece. */
  onDuplicate?: () => void
}

type DialogName = "activate" | "finish" | "archive" | "delete" | "next" | "createNext" | null

export function RoutineActionsBar({
  routine,
  studentId,
  onRoutineChanged,
  mode = "view",
  compact = false,
  onDuplicate,
}: RoutineActionsBarProps) {
  const navigate = useNavigate()
  const toast = useToast()
  const [dialog, setDialog] = useState<DialogName>(null)
  const hasContent = routine.days.length > 0
  const canEdit = routine.status === "ACTIVE" || routine.status === "DRAFT"
  const isFinishedOrArchived =
    routine.status === "FINISHED" || routine.status === "ARCHIVED"

  async function handleDownloadPdf() {
    try {
      const { blob, filename } = await downloadRoutinePdf(routine.id)
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
      console.error(error)
    }
  }

  async function handleCopyWhatsApp() {
    try {
      const text = await getRoutineWhatsAppText(routine.id)
      await navigator.clipboard.writeText(text)
      toast.success("Texto copiado al portapapeles.")
    } catch (error) {
      toast.error("No se pudo copiar. Probá de nuevo.")
      console.error(error)
    }
  }

  function handleSuccess(newRoutine: RoutineResponse) {
    onRoutineChanged()
    navigate(`/students/${studentId}/routines/${newRoutine.id}`)
  }

  return (
    <>
      {compact ? (
        /* ── Modo compacto: para filas de lista ─────────────────────────── */
        <div className="flex items-center gap-1.5">
          {canEdit ? (
            <Button asChild size="sm" className="h-8 px-3 text-xs">
              <Link to={`/students/${studentId}/routines/${routine.id}/edit`}>
                Editar
              </Link>
            </Button>
          ) : (
            <Button asChild size="sm" variant="outline" className="h-8 px-3 text-xs">
              <Link to={`/students/${studentId}/routines/${routine.id}`}>
                Ver
              </Link>
            </Button>
          )}

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="h-8 w-8 px-0"
              >
                <MoreHorizontal className="h-4 w-4" />
                <span className="sr-only">Más acciones</span>
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-52">
              {/* Ver (solo si el primario es Editar) */}
              {canEdit && (
                <DropdownMenuItem asChild>
                  <Link to={`/students/${studentId}/routines/${routine.id}`}>
                    Ver
                  </Link>
                </DropdownMenuItem>
              )}

              <DropdownMenuSeparator />

              <DropdownMenuItem
                disabled={!hasContent}
                onSelect={handleDownloadPdf}
              >
                <Download className="mr-2 h-4 w-4" />
                PDF
              </DropdownMenuItem>
              <DropdownMenuItem
                disabled={!hasContent}
                onSelect={handleCopyWhatsApp}
              >
                <MessageCircle className="mr-2 h-4 w-4" />
                WhatsApp
              </DropdownMenuItem>

              <DropdownMenuSeparator />

              {routine.status === "DRAFT" && (
                <DropdownMenuItem onSelect={() => setDialog("activate")}>
                  <CheckCircle className="mr-2 h-4 w-4" />
                  Activar
                </DropdownMenuItem>
              )}
              {routine.status === "ACTIVE" && (
                <DropdownMenuItem onSelect={() => setDialog("next")}>
                  Finalizar y crear próxima
                </DropdownMenuItem>
              )}
              {routine.status === "ACTIVE" && (
                <DropdownMenuItem onSelect={() => setDialog("finish")}>
                  Solo finalizar
                </DropdownMenuItem>
              )}
              {(routine.status === "ACTIVE" ||
                routine.status === "FINISHED") && (
                <DropdownMenuItem onSelect={() => setDialog("archive")}>
                  <Archive className="mr-2 h-4 w-4" />
                  Archivar
                </DropdownMenuItem>
              )}
              {isFinishedOrArchived && (
                <DropdownMenuItem onSelect={() => setDialog("createNext")}>
                  <Plus className="mr-2 h-4 w-4" />
                  Crear próximo ciclo
                </DropdownMenuItem>
              )}
              {routine.status === "DRAFT" && (
                <DropdownMenuItem
                  onSelect={() => setDialog("delete")}
                  className="text-destructive focus:text-destructive"
                >
                  <Trash2 className="mr-2 h-4 w-4" />
                  Eliminar borrador
                </DropdownMenuItem>
              )}

              {onDuplicate && (
                <>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onSelect={onDuplicate}>
                    Duplicar
                  </DropdownMenuItem>
                </>
              )}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      ) : (
        /* ── Modo completo: para páginas de detalle/view/editor ─────────── */
        <div className="flex flex-wrap items-center gap-2">
          {mode === "view" ? (
            <>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={!hasContent}
                onClick={handleDownloadPdf}
              >
                <Download className="h-4 w-4" />
                PDF
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={!hasContent}
                onClick={handleCopyWhatsApp}
              >
                <MessageCircle className="h-4 w-4" />
                WhatsApp
              </Button>
              {canEdit && (
                <Button asChild size="sm">
                  <Link
                    to={`/students/${studentId}/routines/${routine.id}/edit`}
                  >
                    Editar
                  </Link>
                </Button>
              )}
            </>
          ) : (
            /* mode === "editor" */
            <Button asChild type="button" variant="outline" size="sm">
              <Link to={`/students/${studentId}/routines/${routine.id}`}>
                Ver presentación
              </Link>
            </Button>
          )}

          {routine.status === "DRAFT" && (
            <Button
              type="button"
              size="sm"
              onClick={() => setDialog("activate")}
            >
              <CheckCircle className="h-4 w-4" />
              Activar
            </Button>
          )}

          {isFinishedOrArchived && (
            <Button
              type="button"
              size="sm"
              onClick={() => setDialog("createNext")}
            >
              <Plus className="h-4 w-4" />
              Crear próximo ciclo
            </Button>
          )}

          {(routine.status === "ACTIVE" ||
            routine.status === "DRAFT" ||
            routine.status === "FINISHED") && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button type="button" variant="outline" size="sm">
                  <MoreHorizontal className="h-4 w-4" />
                  Más acciones
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-52">
                {routine.status === "ACTIVE" && (
                  <DropdownMenuItem onSelect={() => setDialog("next")}>
                    Finalizar y crear próxima
                  </DropdownMenuItem>
                )}
                {routine.status === "ACTIVE" && (
                  <DropdownMenuItem onSelect={() => setDialog("finish")}>
                    Solo finalizar
                  </DropdownMenuItem>
                )}
                {(routine.status === "ACTIVE" ||
                  routine.status === "FINISHED") && (
                  <DropdownMenuItem onSelect={() => setDialog("archive")}>
                    <Archive className="mr-2 h-4 w-4" />
                    Archivar
                  </DropdownMenuItem>
                )}
                {routine.status === "DRAFT" && (
                  <DropdownMenuItem
                    onSelect={() => setDialog("delete")}
                    className="text-destructive focus:text-destructive"
                  >
                    <Trash2 className="mr-2 h-4 w-4" />
                    Eliminar borrador
                  </DropdownMenuItem>
                )}
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </div>
      )}

      {/* ── Dialogs compartidos entre ambos modos ──────────────────────────── */}
      <ActivateConfirmDialog
        open={dialog === "activate"}
        onOpenChange={(open) => setDialog(open ? "activate" : null)}
        routine={routine}
        studentId={studentId}
        onSuccess={onRoutineChanged}
      />
      <FinishConfirmDialog
        open={dialog === "finish"}
        onOpenChange={(open) => setDialog(open ? "finish" : null)}
        routineId={routine.id}
        studentId={studentId}
        onSuccess={onRoutineChanged}
      />
      <ArchiveConfirmDialog
        open={dialog === "archive"}
        onOpenChange={(open) => setDialog(open ? "archive" : null)}
        routineId={routine.id}
        studentId={studentId}
        onSuccess={onRoutineChanged}
      />
      <DeleteDraftConfirmDialog
        open={dialog === "delete"}
        onOpenChange={(open) => setDialog(open ? "delete" : null)}
        routineId={routine.id}
        studentId={studentId}
      />
      <FinishAndCreateNextDialog
        open={dialog === "next"}
        onOpenChange={(open) => setDialog(open ? "next" : null)}
        routine={routine}
        studentId={studentId}
        onSuccess={handleSuccess}
      />
      <CreateNextDialog
        open={dialog === "createNext"}
        onOpenChange={(open) => setDialog(open ? "createNext" : null)}
        routine={routine}
        studentId={studentId}
        onSuccess={handleSuccess}
      />
    </>
  )
}

/* ── Dialog components ───────────────────────────────────────────────────── */

function ActivateConfirmDialog({
  open,
  onOpenChange,
  routine,
  studentId,
  onSuccess,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  routine: RoutineResponse
  studentId: number
  onSuccess: () => void
}) {
  const toast = useToast()
  const mutation = useActivateRoutine(studentId)
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>¿Activar esta rutina?</AlertDialogTitle>
          <AlertDialogDescription>
            Si {routine.studentName} tiene una rutina activa actualmente, pasará
            a finalizada automáticamente.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel asChild>
            <Button type="button" variant="outline" disabled={mutation.isPending}>
              Cancelar
            </Button>
          </AlertDialogCancel>
          <Button
            type="button"
            disabled={mutation.isPending}
            onClick={async () => {
              await mutation.mutateAsync(routine.id)
              toast.success("Rutina activada.")
              onSuccess()
              onOpenChange(false)
            }}
          >
            {mutation.isPending && (
              <Loader2 className="h-4 w-4 animate-spin" />
            )}
            Activar
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}

function FinishConfirmDialog({
  open,
  onOpenChange,
  routineId,
  studentId,
  onSuccess,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  routineId: number
  studentId: number
  onSuccess: () => void
}) {
  const toast = useToast()
  const [closureNotes, setClosureNotes] = useState("")
  const mutation = useFinishRoutine(studentId)
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>¿Finalizar esta rutina?</AlertDialogTitle>
          <AlertDialogDescription>
            Quedará como historial y no podrás editarla.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <label className="space-y-1 text-sm font-medium">
          Nota de cierre (opcional)
          <Textarea
            value={closureNotes}
            disabled={mutation.isPending}
            placeholder="¿Cómo respondió el alumno? ¿Qué se modifica en el próximo ciclo?"
            onChange={(e) => setClosureNotes(e.target.value)}
          />
        </label>
        <AlertDialogFooter>
          <AlertDialogCancel asChild>
            <Button type="button" variant="outline" disabled={mutation.isPending}>
              Cancelar
            </Button>
          </AlertDialogCancel>
          <Button
            type="button"
            disabled={mutation.isPending}
            onClick={async () => {
              await mutation.mutateAsync({ routineId, closureNotes })
              toast.success("Rutina finalizada.")
              onSuccess()
              onOpenChange(false)
              setClosureNotes("")
            }}
          >
            {mutation.isPending && (
              <Loader2 className="h-4 w-4 animate-spin" />
            )}
            Finalizar
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}

function ArchiveConfirmDialog({
  open,
  onOpenChange,
  routineId,
  studentId,
  onSuccess,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  routineId: number
  studentId: number
  onSuccess: () => void
}) {
  const toast = useToast()
  const mutation = useArchiveRoutine(studentId)
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>¿Archivar esta rutina?</AlertDialogTitle>
          <AlertDialogDescription>
            Quedará oculta de las listas principales pero seguirá en el
            historial.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel asChild>
            <Button type="button" variant="outline" disabled={mutation.isPending}>
              Cancelar
            </Button>
          </AlertDialogCancel>
          <Button
            type="button"
            variant="outline"
            disabled={mutation.isPending}
            onClick={async () => {
              await mutation.mutateAsync(routineId)
              toast.success("Rutina archivada.")
              onSuccess()
              onOpenChange(false)
            }}
          >
            {mutation.isPending && (
              <Loader2 className="h-4 w-4 animate-spin" />
            )}
            Archivar
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}

function DeleteDraftConfirmDialog({
  open,
  onOpenChange,
  routineId,
  studentId,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  routineId: number
  studentId: number
}) {
  const navigate = useNavigate()
  const toast = useToast()
  const mutation = useDeleteRoutine(studentId)
  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>¿Eliminar este borrador?</AlertDialogTitle>
          <AlertDialogDescription>
            Esta acción no se puede deshacer.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel asChild>
            <Button type="button" variant="outline" disabled={mutation.isPending}>
              Cancelar
            </Button>
          </AlertDialogCancel>
          <Button
            type="button"
            variant="destructive"
            disabled={mutation.isPending}
            onClick={async () => {
              await mutation.mutateAsync(routineId)
              toast.success("Borrador eliminado.")
              navigate(`/students/${studentId}`)
            }}
          >
            {mutation.isPending && (
              <Loader2 className="h-4 w-4 animate-spin" />
            )}
            Eliminar
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}