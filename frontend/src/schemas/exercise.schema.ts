import { z } from "zod"

const optionalText = (max?: number) => {
  const schema = max ? z.string().max(max) : z.string()
  return z.union([schema, z.literal("")]).optional()
}

export const exerciseFormSchema = z.object({
  name: z.string().min(1, "El nombre es obligatorio.").max(150),
  description: optionalText(),
  technicalNotes: optionalText(),
  defaultMeasurement: z.enum(["REPS_WEIGHT", "REPS_ONLY", "TIME", "DISTANCE", "CIRCUIT_REPS"]),
  videoUrl: optionalText(500),
  imageUrl: optionalText(500),
  tagIds: z.array(z.number()),
})

export type ExerciseFormValues = z.infer<typeof exerciseFormSchema>
