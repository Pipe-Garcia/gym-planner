import { zodResolver } from "@hookform/resolvers/zod"
import { Info, Loader2, Save } from "lucide-react"
import { useEffect, useMemo, useState } from "react"
import { FormProvider, useForm, type FieldErrors, type Resolver } from "react-hook-form"
import { useNavigate, useParams } from "react-router-dom"
import { RoutineActionsBar } from "@/components/routine/RoutineActionsBar"
import { RoutineIdentityHeader } from "@/components/routine/RoutineIdentityHeader"
import { TemplateMetadataForm } from "@/components/template/TemplateMetadataForm"
import { TrainingDaysEditor } from "@/components/template/TrainingDaysEditor"
import { defaultDay, normalizeBlockForSubmit, normalizeBlockOrder } from "@/components/template/formDefaults"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { useAuth } from "@/hooks/useAuth"
import { useRoutineDraft } from "@/hooks/useRoutineDraft"
import { useRoutine, useUpdateRoutine } from "@/hooks/useRoutines"
import { useToast } from "@/hooks/useToast"
import { formatDateEs } from "@/lib/date"
import { loadDraft, removeDraft, sameEditableRoutine, type RoutineDraftEnvelope } from "@/lib/routine-draft"
import { routineFormSchema, type RoutineFormValues } from "@/schemas/template.schema"
import type { Routine, RoutineInput } from "@/types/training"

type ErrorNode = { message?: string; [key: string]: unknown }

export function RoutineEditorPage() {
  const { user } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const studentId = Number(useParams().studentId)
  const routineId = Number(useParams().routineId)
  const routineQuery = useRoutine(routineId)
  const update = useUpdateRoutine(routineId, studentId)
  const form = useForm<RoutineFormValues>({
    resolver: zodResolver(routineFormSchema) as unknown as Resolver<RoutineFormValues>,
    defaultValues: { name: "", description: null, sport: null, objective: null, level: null, assignedDate: new Date().toISOString().slice(0, 10), generalNotes: null, internalNotes: null, status: "DRAFT", days: [defaultDay(1)] },
  })
  const routine = routineQuery.data
  const readOnly = routine?.status === "FINISHED" || routine?.status === "ARCHIVED"
  const [hydratedId, setHydratedId] = useState<number | null>(null)
  const [recovery, setRecovery] = useState<{ routineId: number; draft: RoutineDraftEnvelope } | null>(null)
  const [selectedDay, setSelectedDay] = useState(0)
  const [slowSave, setSlowSave] = useState(false)
  const gymId = user?.gymId
  const userId = user?.id
  const scope = useMemo(() => gymId && userId && routineId > 0 ? { gymId, userId, routineId } : null, [gymId, userId, routineId])
  const ready = hydratedId === routineId && routine?.id === routineId
  const recoveryPending = recovery?.routineId === routineId
  const mutationDisabled = readOnly || update.isPending || recoveryPending || !ready
  const draft = useRoutineDraft(form, scope, selectedDay, Boolean(ready && !recoveryPending && !readOnly))

  useEffect(() => {
    if (!routine || routine.id !== routineId) return
    if (hydratedId !== routineId) {
      const serverValues = routineToForm(routine)
      form.reset(serverValues)
      setSelectedDay(0)
      const stored = scope ? loadDraft(scope) : null
      if (stored && !sameEditableRoutine(stored.formValues, serverValues)) {
        setRecovery({ routineId, draft: stored })
      } else {
        if (stored && scope) removeDraft(scope)
        setRecovery(null)
      }
      setHydratedId(routineId)
    } else if (!recoveryPending && !form.formState.isDirty) {
      form.reset(routineToForm(routine))
    }
  }, [routine, routineId, hydratedId, recoveryPending, scope, form])

  useEffect(() => {
    if (!update.isPending) { setSlowSave(false); return }
    const timer = setTimeout(() => setSlowSave(true), 4000)
    return () => clearTimeout(timer)
  }, [update.isPending])

  useEffect(() => {
    if (routine?.status === "FINISHED" || routine?.status === "ARCHIVED") {
      toast.info("Esta rutina no se puede editar.")
      navigate(`/students/${studentId}/routines/${routine.id}`, { replace: true })
    }
  }, [navigate, routine, studentId, toast])

  async function onSubmit(values: RoutineFormValues) {
    if (mutationDisabled) return
    try {
      await update.mutateAsync(normalizeRoutine(values))
      draft.clearOnSuccess()
      toast.success("Rutina guardada.")
      navigate(`/students/${studentId}/routines/${routineId}`)
    } catch (error) {
      toast.error(formatServerError(error, values))
      console.error("Save routine failed:", error)
    }
  }

  function onInvalid(errors: FieldErrors<RoutineFormValues>) {
    console.error("Validation errors:", errors)
    if (import.meta.env.DEV) console.debug("Routine form values:", form.getValues())
    toast.error(collectFirstError(errors, form.getValues()) ?? "Hay campos sin completar o con errores. Revisa los dias.")
  }

  return (
    <FormProvider {...form}>
      <form className="space-y-6" onSubmit={form.handleSubmit(onSubmit, onInvalid)}>
        <Button type="button" variant="ghost" disabled={update.isPending || recoveryPending} onClick={() => (window.history.length > 1 ? navigate(-1) : navigate(`/students/${studentId}`, { replace: true }))}>
          ← Volver
        </Button>
        {routine ? (
          <div className="mb-6 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <RoutineIdentityHeader routine={routine} />
            <div className="flex flex-wrap justify-start gap-2 lg:justify-end">
            {!readOnly ? (
              <Button type="submit" disabled={mutationDisabled}>
                {update.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                {update.isPending ? "Guardando rutina…" : "Guardar cambios"}
              </Button>
            ) : null}
              <RoutineActionsBar routine={routine} studentId={studentId} mode="editor" disabled={update.isPending || recoveryPending} onRoutineChanged={() => { void routineQuery.refetch() }} />
            </div>
            {!readOnly && <p className="text-sm text-muted-foreground" role="status">{update.isPending ? slowSave ? draft.status === "protected" ? "El servidor está tardando en responder. Tus cambios siguen protegidos." : "El servidor está tardando en responder. Esperá a que finalice el guardado." : "Guardando…" : draft.status === "protected" ? "Sin guardar · borrador protegido" : draft.status === "unsaved" ? "Sin guardar" : "Guardado"}</p>}
          </div>
        ) : null}
        {readOnly && routine ? (
          <div className="space-y-3 rounded-md border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
            <div className="flex items-start gap-2">
              <Info className="mt-0.5 h-4 w-4 shrink-0" />
              <div>
                <p className="font-medium">Esta rutina fue finalizada el {formatDateEs(routine.finishedAt ?? routine.finishedDate)}. No se puede editar.</p>
                {routine.closureNotes ? <p className="mt-2 whitespace-pre-wrap">Nota de cierre: {routine.closureNotes}</p> : null}
              </div>
            </div>
            <Button type="button" variant="outline" onClick={() => navigate(`/students/${studentId}/routines/new`, { state: { fromRoutineId: routine.id } })}>
              Crear próxima rutina desde esta →
            </Button>
          </div>
        ) : null}
        {ready && <TemplateMetadataForm routine readOnly={mutationDisabled} />}
        {ready && <TrainingDaysEditor context="routine" disabled={mutationDisabled} studentId={studentId} excludeRoutineId={routineId || null} selectedDay={selectedDay} onSelectedDayChange={setSelectedDay} />}
      </form>
      <Dialog open={Boolean(recoveryPending)} onOpenChange={() => {}}>
        <DialogContent showCloseButton={false} onEscapeKeyDown={(event) => event.preventDefault()} onPointerDownOutside={(event) => event.preventDefault()} onInteractOutside={(event) => event.preventDefault()}>
          <DialogHeader>
            <DialogTitle>Recuperar cambios sin guardar</DialogTitle>
            <DialogDescription>{recoveryPending ? draftDescription(recovery!.draft) : ""}</DialogDescription>
          </DialogHeader>
          <div className="flex flex-wrap justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => { if (scope) removeDraft(scope); setRecovery(null) }}>Descartar</Button>
            <Button type="button" onClick={() => {
              if (!recoveryPending) return
              const restored = recovery!.draft
              form.reset(restored.formValues, { keepDefaultValues: true })
              setSelectedDay(Math.min(restored.selectedDay, Math.max(restored.formValues.days.length - 1, 0)))
              setRecovery(null)
            }}>Recuperar</Button>
          </div>
        </DialogContent>
      </Dialog>
    </FormProvider>
  )
}

function draftDescription(draft: RoutineDraftEnvelope): string {
  const minutes = Math.max(1, Math.floor((Date.now() - draft.savedAt) / 60000))
  const age = minutes < 60 ? `hace ${minutes} minutos` : `hace ${Math.floor(minutes / 60)} horas`
  const days = draft.formValues.days.length
  const exercises = draft.formValues.days.reduce((total, day) => total + day.blocks.reduce((subtotal, block) => subtotal + block.exercises.length, 0), 0)
  return `Encontramos cambios sin guardar de ${age}. ${days} días · ${exercises} ejercicios.`
}

function routineToForm(routine: Routine): RoutineFormValues {
  return {
    studentId: routine.studentId,
    name: routine.name,
    description: null,
    sport: null,
    objective: routine.objective ?? null,
    level: null,
    assignedDate: routine.assignedDate,
    finishedDate: routine.finishedDate ?? null,
    generalNotes: routine.generalNotes ?? null,
    internalNotes: routine.internalNotes ?? null,
    status: routine.status,
    days: routine.days.map((day, dayIndex) => ({
      id: day.id,
      orderIndex: dayIndex + 1,
      name: day.name,
      notes: day.notes ?? null,
      blocks: day.blocks.map((block, blockIndex) => ({
        orderIndex: blockIndex + 1,
        title: block.title,
        structuralType: block.structuralType,
        purpose: block.purpose ?? null,
        totalDurationSeconds: block.totalDurationSeconds ?? null,
        targetRounds: block.targetRounds ?? null,
        roundRestSeconds: block.roundRestSeconds ?? null,
        blockNotes: block.blockNotes ?? null,
        exercises: block.exercises.map((exercise, exerciseIndex) => ({
          exerciseId: exercise.exerciseId,
          exerciseName: exercise.exerciseName,
          exerciseMeasurement: exercise.defaultMeasurement ?? exercise.exercise?.defaultMeasurement ?? "REPS_WEIGHT",
          orderIndex: exerciseIndex + 1,
          exerciseNotes: exercise.exerciseNotes ?? null,
          sets: exercise.sets.map((set, setIndex) => ({ ...set, setNumber: setIndex + 1 })),
        })),
      })),
    })),
  }
}

function normalizeRoutine(values: RoutineFormValues): RoutineInput {
  return {
    ...values,
    days: values.days.map((day, dayIndex) => ({
      ...day,
      orderIndex: dayIndex + 1,
      notes: day.notes ?? null,
      blocks: normalizeBlockOrder(day.blocks).map((block, blockIndex) => ({
        ...normalizeBlockForSubmit(block, blockIndex + 1, "routine"),
        blockNotes: null,
      })),
    })),
  }
}

function collectFirstError(errors: unknown, values?: RoutineFormValues): string | null {
  if (!isErrorNode(errors)) return null

  if (Array.isArray(errors.days)) {
    for (let i = 0; i < errors.days.length; i++) {
      const dayError = errors.days[i]
      if (!dayError) continue
      const dayName = values?.days?.[i]?.name?.trim() || `Dia ${i + 1}`
      const contextMsg = findMessageInDay(dayError, values?.days?.[i])
      if (contextMsg) return `${dayName} · ${contextMsg}`
    }
  }

  return findFirstMessage(errors)
}

function findMessageInDay(dayError: unknown, dayValues?: RoutineFormValues["days"][number]): string | null {
  if (!isErrorNode(dayError)) return null
  if (isMessageNode(dayError.name)) return dayError.name.message
  if (isMessageNode(dayError.notes)) return dayError.notes.message

  if (Array.isArray(dayError.blocks)) {
    for (let i = 0; i < dayError.blocks.length; i++) {
      const blockError = dayError.blocks[i]
      if (!blockError) continue
      const blockTitle = dayValues?.blocks?.[i]?.title?.trim() || `Bloque ${i + 1}`
      const msg = findMessageInBlock(blockError, dayValues?.blocks?.[i])
      if (msg) return `Bloque "${blockTitle}": ${msg}`
    }
  } else if (isMessageNode(dayError.blocks)) {
    return dayError.blocks.message
  }

  return findFirstMessage(dayError)
}

function findMessageInBlock(blockError: unknown, blockValues?: RoutineFormValues["days"][number]["blocks"][number]): string | null {
  if (!isErrorNode(blockError)) return null
  if (isMessageNode(blockError.title)) return blockError.title.message
  if (isMessageNode(blockError.totalDurationSeconds)) return blockError.totalDurationSeconds.message
  if (isMessageNode(blockError.targetRounds)) return blockError.targetRounds.message
  if (isMessageNode(blockError.roundRestSeconds)) return blockError.roundRestSeconds.message
  if (isMessageNode(blockError.structuralType)) return blockError.structuralType.message
  if (isMessageNode(blockError.purpose)) return blockError.purpose.message

  if (Array.isArray(blockError.exercises)) {
    for (let i = 0; i < blockError.exercises.length; i++) {
      const exerciseError = blockError.exercises[i]
      if (!exerciseError) continue
      const exerciseName = blockValues?.exercises?.[i]?.exerciseName || `Ejercicio ${i + 1}`
      const msg = findFirstMessage(exerciseError)
      if (msg) return `Ejercicio "${exerciseName}": ${msg}`
    }
  } else if (isMessageNode(blockError.exercises)) {
    return blockError.exercises.message
  }

  return findFirstMessage(blockError)
}

function findFirstMessage(node: unknown): string | null {
  if (!isErrorNode(node)) return null
  if (isMessageNode(node)) return node.message
  for (const value of Object.values(node)) {
    const result = findFirstMessage(value)
    if (result) return result
  }
  return null
}

function formatServerError(error: unknown, values: RoutineFormValues): string {
  const message = errorMessage(error)
  const dayName = values.days.find((day) => day.name?.trim() && message.includes(day.name.trim()))?.name.trim()
  return dayName ? `${dayName} · ${message}` : `No se pudo guardar la rutina: ${message}`
}

function isErrorNode(node: unknown): node is ErrorNode {
  return !!node && typeof node === "object"
}

function isMessageNode(node: unknown): node is { message: string } {
  return isErrorNode(node) && typeof node.message === "string"
}

function errorMessage(error: unknown) {
  if (error && typeof error === "object" && "response" in error) {
    const response = error.response
    if (response && typeof response === "object" && "data" in response) {
      const data = response.data
      if (data && typeof data === "object" && "message" in data && typeof data.message === "string") return data.message
    }
  }
  return error instanceof Error ? error.message : "Error al guardar"
}
