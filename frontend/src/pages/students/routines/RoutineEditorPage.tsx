import { zodResolver } from "@hookform/resolvers/zod"
import { Archive, CheckCircle, Save, Trash2 } from "lucide-react"
import { useEffect } from "react"
import { FormProvider, useForm, type FieldErrors, type Resolver } from "react-hook-form"
import { useNavigate, useParams } from "react-router-dom"
import { BackButton } from "@/components/shared/BackButton"
import { TemplateMetadataForm } from "@/components/template/TemplateMetadataForm"
import { TrainingDaysEditor } from "@/components/template/TrainingDaysEditor"
import { defaultDay, normalizeBlockForSubmit, normalizeBlockOrder } from "@/components/template/formDefaults"
import { Button } from "@/components/ui/button"
import { useRoutine, useRoutineAction, useUpdateRoutine } from "@/hooks/useRoutines"
import { useToast } from "@/hooks/useToast"
import { routineStatusBadgeClass, routineStatusLabel } from "@/lib/labels"
import { routineFormSchema, type RoutineFormValues } from "@/schemas/template.schema"
import type { Routine, RoutineInput } from "@/types/training"

type ErrorNode = { message?: string; [key: string]: unknown }

export function RoutineEditorPage() {
  const toast = useToast()
  const navigate = useNavigate()
  const studentId = Number(useParams().studentId)
  const routineId = Number(useParams().routineId)
  const routineQuery = useRoutine(routineId)
  const update = useUpdateRoutine(routineId, studentId)
  const actions = useRoutineAction(studentId)
  const form = useForm<RoutineFormValues>({
    resolver: zodResolver(routineFormSchema) as unknown as Resolver<RoutineFormValues>,
    defaultValues: { name: "", description: null, sport: null, objective: null, level: null, assignedDate: new Date().toISOString().slice(0, 10), generalNotes: null, internalNotes: null, status: "DRAFT", days: [defaultDay(1)] },
  })
  const routine = routineQuery.data
  const readOnly = routine?.status === "FINISHED" || routine?.status === "ARCHIVED"

  useEffect(() => {
    if (routine) form.reset(routineToForm(routine))
  }, [routine, form])

  async function onSubmit(values: RoutineFormValues) {
    try {
      await update.mutateAsync(normalizeRoutine(values))
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
        <BackButton to={`/students/${studentId}/routines/${routineId}`} />
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-2xl font-semibold tracking-normal">{routine?.name || "Rutina"}</h1>
              <span className={routineStatusBadgeClass(routine?.status)}>{routineStatusLabel(routine?.status)}</span>
            </div>
            <p className="text-base font-medium text-foreground">{routine?.studentName}</p>
          </div>
          <div className="flex flex-wrap gap-2">
            {!readOnly ? (
              <Button type="submit">
                <Save className="h-4 w-4" />
                Guardar
              </Button>
            ) : null}
            {routine?.status === "DRAFT" ? <Button type="button" onClick={async () => { await actions.activate.mutateAsync(routine.id); toast.success("Rutina activada."); await routineQuery.refetch() }}><CheckCircle className="h-4 w-4" />Activar rutina</Button> : null}
            {routine?.status === "ACTIVE" ? <Button type="button" variant="outline" onClick={async () => { await actions.finish.mutateAsync(routine.id); toast.success("Rutina finalizada."); await routineQuery.refetch() }}><CheckCircle className="h-4 w-4" />Finalizar</Button> : null}
            {routine?.status === "ACTIVE" || routine?.status === "FINISHED" ? <Button type="button" variant="outline" onClick={async () => { await actions.archive.mutateAsync(routine.id); toast.success("Rutina archivada."); await routineQuery.refetch() }}><Archive className="h-4 w-4" />Archivar</Button> : null}
            {routine?.status === "DRAFT" ? <Button type="button" variant="destructive" onClick={async () => { await actions.deleteDraft.mutateAsync(routine.id); navigate(`/students/${studentId}`) }}><Trash2 className="h-4 w-4" />Eliminar</Button> : null}
          </div>
        </div>
        {readOnly ? <div className="rounded-md border bg-amber-50 p-4 text-sm text-amber-800">Esta rutina ya finalizo. Para modificarla, duplicala como nueva.</div> : null}
        <TemplateMetadataForm routine readOnly={readOnly} />
        <TrainingDaysEditor context="routine" disabled={readOnly} />
      </form>
    </FormProvider>
  )
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
