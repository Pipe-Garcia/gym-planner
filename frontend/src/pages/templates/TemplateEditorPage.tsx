import { zodResolver } from "@hookform/resolvers/zod"
import { Save } from "lucide-react"
import { useEffect } from "react"
import { FormProvider, useForm, useWatch, type FieldErrors, type Resolver } from "react-hook-form"
import { useNavigate, useParams } from "react-router-dom"
import { BackButton } from "@/components/shared/BackButton"
import { TemplateMetadataForm } from "@/components/template/TemplateMetadataForm"
import { TrainingDaysEditor } from "@/components/template/TrainingDaysEditor"
import { defaultDay, normalizeBlockForSubmit, normalizeBlockOrder } from "@/components/template/formDefaults"
import { Button } from "@/components/ui/button"
import { useCreateTemplate, useTemplate, useUpdateTemplate } from "@/hooks/useTemplates"
import { useToast } from "@/hooks/useToast"
import { templateFormSchema, type TemplateFormValues } from "@/schemas/template.schema"
import type { Template, TemplateInput } from "@/types/training"

type ErrorNode = { message?: string; [key: string]: unknown }

const defaultValues: TemplateFormValues = {
  name: "",
  description: null,
  sport: null,
  objective: null,
  level: null,
  generalNotes: null,
  days: [defaultDay(1)],
}

export function TemplateEditorPage() {
  const toast = useToast()
  const navigate = useNavigate()
  const idParam = useParams().id
  const id = idParam && idParam !== "new" ? Number(idParam) : undefined
  const templateQuery = useTemplate(id)
  const create = useCreateTemplate()
  const update = useUpdateTemplate(id ?? 0)
  const form = useForm<TemplateFormValues>({ resolver: zodResolver(templateFormSchema) as unknown as Resolver<TemplateFormValues>, defaultValues })
  const watchedName = useWatch({ control: form.control, name: "name" })

  useEffect(() => {
    if (templateQuery.data) form.reset(templateToForm(templateQuery.data))
  }, [templateQuery.data, form])

  async function onSubmit(values: TemplateFormValues) {
    try {
      const saved = id ? await update.mutateAsync(normalizePayload(values)) : await create.mutateAsync(normalizePayload(values))
      toast.success("Plantilla guardada.")
      navigate(`/templates/${saved.id}`)
    } catch (error) {
      toast.error(formatServerError(error, values))
      console.error("Save template failed:", error)
    }
  }

  function onInvalid(errors: FieldErrors<TemplateFormValues>) {
    console.error("Validation errors:", errors)
    if (import.meta.env.DEV) console.debug("Template form values:", form.getValues())
    toast.error(collectFirstError(errors, form.getValues()) ?? "Hay campos sin completar o con errores. Revisa los dias.")
  }

  return (
    <FormProvider {...form}>
      <form className="space-y-6" onSubmit={form.handleSubmit(onSubmit, onInvalid)}>
        <BackButton to={id ? `/templates/${id}` : "/templates"} />
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-semibold tracking-normal">
              {id ? "Editar plantilla" : "Nueva plantilla"}
              {watchedName?.trim() ? <span className="ml-2 text-muted-foreground">— {watchedName}</span> : null}
            </h1>
            <p className="text-sm text-muted-foreground">Dias, bloques, ejercicios y sets explicitos.</p>
          </div>
          <Button type="submit">
            <Save className="h-4 w-4" />
            Guardar plantilla
          </Button>
        </div>
        <TemplateMetadataForm />
        <TrainingDaysEditor context="template" />
      </form>
    </FormProvider>
  )
}

function templateToForm(template: Template): TemplateFormValues {
  return {
    name: template.name,
    description: template.description ?? null,
    sport: template.sport ?? null,
    objective: template.objective ?? null,
    level: template.level ?? null,
    generalNotes: template.generalNotes ?? null,
    days: template.days.map((day, dayIndex) => ({
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
        blockNotes: null,
        exercises: block.exercises.map((exercise, exerciseIndex) => ({
          exerciseId: exercise.exerciseId,
          exerciseName: exercise.exerciseName,
          exerciseMeasurement: exercise.defaultMeasurement ?? exercise.exercise?.defaultMeasurement ?? "REPS_WEIGHT",
          orderIndex: exerciseIndex + 1,
          exerciseNotes: exercise.exerciseNotes ?? null,
          sets: exercise.sets.map((set, setIndex) => ({ ...set, targetWeightKg: null, setNumber: setIndex + 1 })),
        })),
      })),
    })),
  }
}

function normalizePayload(values: TemplateFormValues): TemplateInput {
  return {
    ...values,
    estimatedDurationMinutes: null,
    days: values.days.map((day, dayIndex) => ({
      ...day,
      orderIndex: dayIndex + 1,
      notes: day.notes ?? null,
      blocks: normalizeBlockOrder(day.blocks).map((block, blockIndex) => ({
        ...normalizeBlockForSubmit(block, blockIndex + 1, "template"),
        blockNotes: null,
      })),
    })),
  }
}

function collectFirstError(errors: unknown, values?: TemplateFormValues): string | null {
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

function findMessageInDay(dayError: unknown, dayValues?: TemplateFormValues["days"][number]): string | null {
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

function findMessageInBlock(blockError: unknown, blockValues?: TemplateFormValues["days"][number]["blocks"][number]): string | null {
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

function formatServerError(error: unknown, values: TemplateFormValues): string {
  const message = errorMessage(error)
  const dayName = values.days.find((day) => day.name?.trim() && message.includes(day.name.trim()))?.name.trim()
  return dayName ? `${dayName} · ${message}` : `No se pudo guardar la plantilla: ${message}`
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
