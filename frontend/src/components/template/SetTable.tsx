import { useMemo, useState } from "react"
import { useFieldArray, useFormContext } from "react-hook-form"
import { AdvancedSetEditor } from "@/components/template/AdvancedSetEditor"
import { SimpleSetForm } from "@/components/template/SimpleSetForm"
import type { SetFormValue } from "@/schemas/template.schema"
import { cn } from "@/lib/utils"
import type { MeasurementType } from "@/types/exercise"

function allSetsAreEqual(sets: SetFormValue[]): boolean {
  if (sets.length <= 1) return true
  const [first, ...rest] = sets
  return rest.every(
    (set) =>
      set.targetReps === first.targetReps &&
      set.targetWeightKg === first.targetWeightKg &&
      set.restAfterSeconds === first.restAfterSeconds &&
      set.targetTimeSeconds === first.targetTimeSeconds &&
      set.targetDistanceMeters === first.targetDistanceMeters &&
      (set.setKind ?? "NORMAL") === "NORMAL",
  )
}

interface SetTableProps {
  name: string
  measurement: MeasurementType
  context: "template" | "routine"
  disabled?: boolean
}

export function SetTable({ name, measurement, context, disabled }: SetTableProps) {
  const { control, getValues } = useFormContext()
  const setsField = useFieldArray({ control, name })

  // CRITICAL: read once at mount. Subscribing with watch here makes set inputs
  // re-render on every keystroke and can desync React Hook Form state.
  const initialMode = useMemo<"simple" | "advanced">(() => {
    const initial = (getValues(name) ?? []) as SetFormValue[]
    return allSetsAreEqual(initial) ? "simple" : "advanced"
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const [mode, setMode] = useState<"simple" | "advanced">(initialMode)

  return (
    <div className="space-y-3">
      <div className="inline-flex rounded-md border bg-white p-1">
        <button
          type="button"
          className={cn("rounded px-3 py-1.5 text-sm", mode === "simple" && "bg-primary text-primary-foreground")}
          onClick={() => setMode("simple")}
        >
          Simple
        </button>
        <button
          type="button"
          className={cn("rounded px-3 py-1.5 text-sm", mode === "advanced" && "bg-primary text-primary-foreground")}
          onClick={() => setMode("advanced")}
        >
          Avanzado
        </button>
      </div>

      <div className={mode === "simple" ? "" : "hidden"}>
        <SimpleSetForm setsField={setsField} sets={getValues(name) as SetFormValue[] | undefined} measurement={measurement} context={context} disabled={disabled} active={mode === "simple"} />
      </div>
      <div className={mode === "advanced" ? "" : "hidden"}>
        <AdvancedSetEditor name={name} setsField={setsField} measurement={measurement} context={context} disabled={disabled} />
      </div>
    </div>
  )
}
