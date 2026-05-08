import { z } from "zod"

export const loginSchema = z.object({
  email: z.string().email("Ingresá un email válido."),
  password: z.string().min(1, "Ingresá tu password."),
})

export type LoginFormValues = z.infer<typeof loginSchema>
