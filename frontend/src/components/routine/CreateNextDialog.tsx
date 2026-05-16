import { zodResolver } from "@hookform/resolvers/zod"
import { Loader2 } from "lucide-react"
import { useEffect } from "react"
import { useForm, type Resolver } from "react-hook-form"
import { z } from "zod"
import { NewRoutineFields, suggestNextCycleName, todayInputValue, WeightAdjustmentFields, type CycleFormFields } from "@/components/routine/RoutineCycleFields"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Separator } from "@/components/ui/separator"
import { useCreateNextRoutine } from "@/hooks/useRoutines"
import { useToast } from "@/hooks/useToast"
import type { CreateNextRoutineInput, RoutineResponse } from "@/types/training"

const createNextSchema = z.object({
  newRoutineName: z.string().min(1, "El nombre es obligatorio").max(150),
  newAssignedDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Usa formato YYYY-MM-DD"),
  newStatus: z.enum(["DRAFT", "ACTIVE"]),
  copyGeneralNotes: z.boolean(),
  copyInternalNotes: z.boolean(),
  applyWeightAdjustment: z.boolean(),
  weightPercentage: z.number().min(-90).max(900),
  roundingStepKg: z.number().nullable(),
})

type CreateNextValues = z.infer<typeof createNextSchema> & CycleFormFields

interface CreateNextDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  routine: RoutineResponse
  studentId: number
  onSuccess: (newRoutine: RoutineResponse) => void
}

export function CreateNextDialog({ open, onOpenChange, routine, studentId, onSuccess }: CreateNextDialogProps) {
  const toast = useToast()
  const mutation = useCreateNextRoutine(studentId)
  const form = useForm<CreateNextValues>({
    resolver: zodResolver(createNextSchema) as unknown as Resolver<CreateNextValues>,
    defaultValues: defaultValues(routine),
  })

  useEffect(() => {
    if (open) form.reset(defaultValues(routine))
  }, [form, open, routine])

  async function onSubmit(values: CreateNextValues) {
    const data: CreateNextRoutineInput = {
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
    const response = await mutation.mutateAsync({ sourceRoutineId: routine.id, data })
    toast.success(`Nuevo ciclo "${response.newRoutine.name}" creado.`)
    if (response.newRoutine.status === "ACTIVE") toast.info("La nueva rutina ya está activa.")
    onSuccess(response.newRoutine)
    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Crear próximo ciclo</DialogTitle>
          <DialogDescription>Creá una nueva rutina enlazada a este ciclo cerrado.</DialogDescription>
        </DialogHeader>
        <form className="space-y-5" onSubmit={form.handleSubmit(onSubmit)}>
          <section className="space-y-3">
            <h3 className="text-sm font-semibold">Nuevo ciclo</h3>
            <NewRoutineFields form={form} disabled={mutation.isPending} />
          </section>
          <Separator />
          <WeightAdjustmentFields form={form} disabled={mutation.isPending} />
          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button type="button" variant="outline" disabled={mutation.isPending} onClick={() => onOpenChange(false)}>Cancelar</Button>
            <Button type="submit" disabled={mutation.isPending}>{mutation.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}Crear próximo ciclo</Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function defaultValues(routine: RoutineResponse): CreateNextValues {
  return {
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
