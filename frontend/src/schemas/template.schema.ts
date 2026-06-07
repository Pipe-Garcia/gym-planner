import { z } from "zod"

const isEmpty = (value: unknown) =>
  value === "" || value === undefined || value === null || (typeof value === "number" && Number.isNaN(value))

const nullableNumber = z.preprocess((value) => (isEmpty(value) ? null : value), z.number().nullable())
const nullablePositiveInt = z.preprocess((value) => (isEmpty(value) ? null : value), z.number().int().positive().nullable())
const nullableNonNegativeInt = z.preprocess((value) => (isEmpty(value) ? null : value), z.number().int().nonnegative().nullable())
const nullableText = z.preprocess((value) => (value === "" || value === undefined ? null : value), z.string().nullable())
const nullableExecutionCue = z.preprocess((value) => {
  if (value === undefined || value === null) return null
  if (typeof value !== "string") return value
  const trimmed = value.trim()
  return trimmed === "" ? null : trimmed
}, z.string().max(120).nullable())
const nullablePositiveNumber = z.preprocess((value) => (isEmpty(value) ? null : value), z.number().positive().nullable())
const nullableRpe = z.preprocess((value) => (isEmpty(value) ? null : value), z.number().int().min(1).max(10).nullable())
const technicalPositiveInt = z.preprocess((value) => (isEmpty(value) ? 1 : value), z.number().int().positive())
const setKindSchema = z.preprocess((value) => (isEmpty(value) ? "NORMAL" : value), z.enum(["NORMAL", "WARMUP", "FAILURE", "DROP", "REST_PAUSE_PORTION"]))
const measurementSchema = z.preprocess((value) => (isEmpty(value) ? "REPS_WEIGHT" : value), z.enum(["REPS_WEIGHT", "REPS_ONLY", "TIME", "DISTANCE", "CIRCUIT_REPS"]))
const exerciseIdSchema = z.preprocess((value) => (isEmpty(value) ? undefined : value), z.number({ error: "Selecciona un ejercicio" }))

export const setSchema = z.object({
  setNumber: technicalPositiveInt,
  setKind: setKindSchema,
  targetReps: nullablePositiveInt,
  targetRepsMin: nullablePositiveInt.optional(),
  targetRepsMax: nullablePositiveInt.optional(),
  targetWeightKg: nullablePositiveNumber,
  targetTimeSeconds: nullablePositiveInt,
  targetDistanceMeters: nullableNumber.optional(),
  restAfterSeconds: nullableNonNegativeInt,
  tempo: nullableText,
  executionCue: nullableExecutionCue.optional(),
  rpe: nullableRpe,
  notes: nullableText,
  toFailure: z.preprocess((value) => (isEmpty(value) ? false : value), z.boolean()),
})

export const exerciseInBlockSchema = z.object({
  exerciseId: exerciseIdSchema,
  exerciseName: z.string().optional(),
  exerciseMeasurement: measurementSchema,
  orderIndex: technicalPositiveInt,
  exerciseNotes: nullableText,
  sets: z.array(setSchema).min(1, "El ejercicio debe tener al menos un set"),
})

const blockBaseSchema = z.object({
  orderIndex: technicalPositiveInt,
  title: z.string().min(1, "El bloque debe tener un titulo").max(150),
  structuralType: z.enum(["STANDARD", "CIRCUIT", "GROUPED_SET", "PYRAMID", "REVERSE_PYRAMID", "DROP_SET", "REST_PAUSE", "CLUSTER"]),
  purpose: z
    .preprocess(
      (value) => (isEmpty(value) ? null : value),
      z.enum(["WARMUP", "ACTIVATION", "MAIN_LIFT", "ACCESSORY", "CONDITIONING", "CORE", "COOLDOWN", "OTHER"]).nullable()
    ),
  totalDurationSeconds: nullablePositiveInt,
  targetRounds: nullablePositiveInt,
  roundRestSeconds: nullableNonNegativeInt,
  blockNotes: nullableText,
  exercises: z.array(exerciseInBlockSchema).min(1, "El bloque debe tener al menos un ejercicio"),
})

export const blockSchema = blockBaseSchema
  .refine((block) => block.structuralType !== "CIRCUIT" || block.totalDurationSeconds != null, {
    message: "Los bloques de tipo Circuito requieren duracion total",
    path: ["totalDurationSeconds"],
  })
  .refine((block) => block.structuralType !== "GROUPED_SET" || block.targetRounds != null, {
    message: "Las series agrupadas requieren vueltas",
    path: ["targetRounds"],
  })

const draftBlockSchema = blockBaseSchema.extend({
  exercises: z.array(exerciseInBlockSchema.extend({ sets: z.array(setSchema) })),
})

export const daySchema = z.object({
  id: z.number().optional(),
  orderIndex: z.number().int().positive().optional(),
  name: z.string().min(1, "El dia debe tener un nombre").max(150),
  notes: nullableText,
  blocks: z.array(blockSchema),
})

const draftDaySchema = daySchema.extend({
  blocks: z.array(draftBlockSchema),
})

export const templateFormSchema = z.object({
  name: z.string().min(1, "La plantilla debe tener un nombre").max(150),
  description: nullableText,
  sport: nullableText,
  objective: nullableText,
  level: nullableText,
  generalNotes: nullableText,
  days: z.array(daySchema).min(1, "La plantilla debe tener al menos un dia"),
})

export const routineFormSchema = templateFormSchema.extend({
  studentId: z.number().optional(),
  assignedDate: z.string().min(1),
  finishedDate: nullableText.optional(),
  internalNotes: nullableText,
  status: z.enum(["ACTIVE", "FINISHED", "ARCHIVED", "DRAFT"]).optional(),
  days: z.array(draftDaySchema).min(1, "La rutina debe tener al menos un dia"),
})

export type TemplateFormValues = z.infer<typeof templateFormSchema>
export type RoutineFormValues = z.infer<typeof routineFormSchema>
export type TrainingFormValues = TemplateFormValues | RoutineFormValues
export type SetFormValue = z.infer<typeof setSchema>
export type DayFormValue = z.infer<typeof daySchema>
