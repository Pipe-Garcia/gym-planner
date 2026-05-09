import { z } from "zod"

const optionalText = (max?: number) => {
  const schema = max ? z.string().max(max) : z.string()
  return z.union([schema, z.literal("")]).optional()
}

const optionalDate = z.union([z.string().date(), z.literal("")]).optional()

export const studentFormSchema = z.object({
  firstName: z.string().min(1, "El nombre es obligatorio.").max(100),
  lastName: z.string().min(1, "El apellido es obligatorio.").max(100),
  documentId: optionalText(50),
  phone: optionalText(50),
  email: z.union([z.string().email("Ingresa un email valido."), z.literal("")]).optional(),
  birthDate: optionalDate,
  sport: optionalText(100),
  objective: optionalText(150),
  level: optionalText(50),
  generalNotes: optionalText(),
  startedAt: optionalDate,
})

export const injuryFormSchema = z.object({
  bodyArea: z.string().min(1, "La zona es obligatoria.").max(100),
  description: z.string().min(1, "La descripcion es obligatoria."),
  severity: z.enum(["LEVE", "MODERADA", "GRAVE"]),
  startedAt: optionalDate,
  notes: optionalText(),
})

export const noteFormSchema = z.object({
  content: z.string().min(1, "La nota no puede estar vacia."),
})

export type StudentFormValues = z.infer<typeof studentFormSchema>
export type InjuryFormValues = z.infer<typeof injuryFormSchema>
export type NoteFormValues = z.infer<typeof noteFormSchema>
