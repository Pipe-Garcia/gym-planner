import { useFormContext } from "react-hook-form"
import { RoutineNotesTabs } from "@/components/template/RoutineNotesTabs"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"

export function TemplateMetadataForm({ routine = false, readOnly = false }: { routine?: boolean; readOnly?: boolean }) {
  const { register } = useFormContext()

  if (routine) {
    return (
      <div className="rounded-md border bg-background p-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <label className="space-y-1 text-sm font-medium">
            Nombre
            <Input disabled={readOnly} {...register("name")} />
          </label>
          <label className="space-y-1 text-sm font-medium">
            Objetivo
            <Input disabled={readOnly} {...register("objective")} />
          </label>
          <label className="space-y-1 text-sm font-medium">
            Fecha asignada
            <Input type="date" disabled={readOnly} {...register("assignedDate")} />
          </label>
          <div className="space-y-1 text-sm font-medium">
            <span>Notas</span>
            <RoutineNotesTabs disabled={readOnly} />
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="grid gap-4 rounded-md border bg-white p-4 sm:grid-cols-2 lg:grid-cols-4">
      <label className="space-y-1 text-sm font-medium sm:col-span-2">Nombre<Input disabled={readOnly} {...register("name")} /></label>
      <label className="space-y-1 text-sm font-medium">Deporte<Input disabled={readOnly} {...register("sport")} /></label>
      <label className="space-y-1 text-sm font-medium">Objetivo<Input disabled={readOnly} {...register("objective")} /></label>
      <label className="space-y-1 text-sm font-medium">Nivel<Input disabled={readOnly} {...register("level")} /></label>
      <label className="space-y-1 text-sm font-medium sm:col-span-2 lg:col-span-4">Descripcion<Input disabled={readOnly} {...register("description")} /></label>
      <label className="space-y-1 text-sm font-medium sm:col-span-2 lg:col-span-4">
        Notas generales
        <Textarea disabled={readOnly} {...register("generalNotes")} />
      </label>
    </div>
  )
}
