import { zodResolver } from "@hookform/resolvers/zod"
import { Loader2 } from "lucide-react"
import { useEffect } from "react"
import { useForm, type Resolver } from "react-hook-form"
import { z } from "zod"
import { NewRoutineFields, suggestNextCycleName, todayInputValue, WeightAdjustmentFields, type CycleFormFields } from "@/components/routine/RoutineCycleFields"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Separator } from "@/components/ui/separator"
import { Textarea } from "@/components/ui/textarea"
import { useFinishAndCreateNext } from "@/hooks/useRoutines"
import { useToast } from "@/hooks/useToast"
import type { FinishAndCreateNextInput, RoutineResponse } from "@/types/training"

const datePattern = /^\d{4}-\d{2}-\d{2}$/

const finishAndCreateNextSchema = z.object({
  closureNotes: z.string().optional(),
  newRoutineName: z.string().min(1, "El nombre es obligatorio").max(150),
  newAssignedDate: z.string().regex(datePattern, "Usa formato YYYY-MM-DD"),
  newStatus: z.enum(["DRAFT", "ACTIVE"]),
  copyGeneralNotes: z.boolean(),
  copyInternalNotes: z.boolean(),
  applyWeightAdjustment: z.boolean(),
  weightPercentage: z.number().min(-90).max(900),
  roundingStepKg: z.number().nullable(),
})

type FinishAndCreateNextValues = z.infer<typeof finishAndCreateNextSchema> & CycleFormFields

interface FinishAndCreateNextDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  routine: RoutineResponse
  studentId: number
  onSuccess: (newRoutine: RoutineResponse) => void
}

export function FinishAndCreateNextDialog({ open, onOpenChange, routine, studentId, onSuccess }: FinishAndCreateNextDialogProps) {
  const toast = useToast()
  const mutation = useFinishAndCreateNext(studentId)
  const form = useForm<FinishAndCreateNextValues>({
    resolver: zodResolver(finishAndCreateNextSchema) as unknown as Resolver<FinishAndCreateNextValues>,
    defaultValues: defaultValues(routine),
  })

  useEffect(() => {
    if (open) form.reset(defaultValues(routine))
  }, [form, open, routine])

  async function onSubmit(values: FinishAndCreateNextValues) {
    const input: FinishAndCreateNextInput = {
      routineId: routine.id,
      closureNotes: emptyToUndefined(values.closureNotes),
      newRoutineName: values.newRoutineName.trim(),
      newAssignedDate: values.newAssignedDate,
      newStatus: values.newStatus,
      copyGeneralNotes: values.copyGeneralNotes,
      copyInternalNotes: values.copyInternalNotes,
      weightAdjustment: values.applyWeightAdjustment
        ? {
            percentage: values.weightPercentage,
            roundingStepKg: values.roundingStepKg ?? undefined,
          }
        : undefined,
    }
    const response = await mutation.mutateAsync(input)
    toast.success(`Ciclo finalizado. Nuevo ciclo "${response.newRoutine.name}" creado.`)
    if (response.newRoutine.status === "ACTIVE") toast.info("La nueva rutina ya está activa.")
    onSuccess(response.newRoutine)
    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Finalizar y crear próxima</DialogTitle>
          <DialogDescription>Cerrá el ciclo actual y prepará la siguiente rutina con una copia independiente.</DialogDescription>
        </DialogHeader>

        <form className="space-y-5" onSubmit={form.handleSubmit(onSubmit)}>
          <section className="space-y-3">
            <h3 className="text-sm font-semibold">Cierre del ciclo actual</h3>
            <label className="space-y-1 text-sm font-medium">
              Nota de cierre (opcional)
              <Textarea
                placeholder="¿Cómo respondió el alumno? ¿Qué cambios hacer en el próximo ciclo?"
                disabled={mutation.isPending}
                {...form.register("closureNotes")}
              />
            </label>
            <p className="text-xs text-muted-foreground">Solo visible para el equipo. No se incluye en PDF ni WhatsApp.</p>
          </section>

          <Separator />

          <section className="space-y-3">
            <h3 className="text-sm font-semibold">Nuevo ciclo</h3>
            <NewRoutineFields form={form} disabled={mutation.isPending} />
          </section>

          <Separator />

          <WeightAdjustmentFields form={form} disabled={mutation.isPending} />

          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button type="button" variant="outline" disabled={mutation.isPending} onClick={() => onOpenChange(false)}>Cancelar</Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              Finalizar y crear próximo ciclo
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function defaultValues(routine: RoutineResponse): FinishAndCreateNextValues {
  return {
    closureNotes: "",
    newRoutineName: suggestNextCycleName(routine.name),
    newAssignedDate: todayInputValue(),
    newStatus: "DRAFT",
    copyGeneralNotes: true,
    copyInternalNotes: false,
    applyWeightAdjustment: false,
    weightPercentage: 5,
    roundingStepKg: 2.5,
  }
}

function emptyToUndefined(value?: string) {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}
