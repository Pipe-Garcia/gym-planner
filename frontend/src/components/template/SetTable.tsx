import { useState } from "react"
import { useFieldArray, useFormContext } from "react-hook-form"
import { AdvancedSetEditor } from "@/components/template/AdvancedSetEditor"
import { SimpleSetForm } from "@/components/template/SimpleSetForm"
import type { SetFormValue } from "@/schemas/template.schema"
import { cn } from "@/lib/utils"
import type { MeasurementType } from "@/types/exercise"

function allSetsAreEqual(sets: SetFormValue[]): boolean {
  if (sets.length === 0) return true
  const [first, ...rest] = sets
  return rest.every((set) => set.targetReps === first.targetReps && set.targetWeightKg === first.targetWeightKg && set.restAfterSeconds === first.restAfterSeconds && set.setKind === "NORMAL" && !set.tempo && !set.rpe && !set.toFailure)
}

interface SetTableProps {
  name: string
  measurement: MeasurementType
  context: "template" | "routine"
  disabled?: boolean
}

export function SetTable({ name, measurement, context, disabled }: SetTableProps) {
  const { control, watch } = useFormContext()
  const sets = watch(name) as SetFormValue[] | undefined
  const [mode, setMode] = useState<"simple" | "advanced">(() => allSetsAreEqual(sets ?? []) ? "simple" : "advanced")
  const setsField = useFieldArray({ control, name })
  return (
    <div className="space-y-3">
      <div className="inline-flex rounded-md border bg-white p-1">
        <button type="button" className={cn("rounded px-3 py-1.5 text-sm", mode === "simple" && "bg-primary text-primary-foreground")} onClick={() => setMode("simple")}>Simple</button>
        <button type="button" className={cn("rounded px-3 py-1.5 text-sm", mode === "advanced" && "bg-primary text-primary-foreground")} onClick={() => setMode("advanced")}>Avanzado</button>
      </div>
      {mode === "simple" ? (
        <SimpleSetForm setsField={setsField} sets={sets} measurement={measurement} context={context} disabled={disabled} />
      ) : (
        <AdvancedSetEditor name={name} setsField={setsField} measurement={measurement} context={context} disabled={disabled} />
      )}
    </div>
  )
}
