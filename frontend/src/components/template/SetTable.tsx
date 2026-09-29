import { useEffect, useState } from "react"
import { useFieldArray, useFormContext, useWatch } from "react-hook-form"
import { AdvancedSetEditor } from "@/components/template/AdvancedSetEditor"
import { SimpleSetForm } from "@/components/template/SimpleSetForm"
import { isSimpleRepresentable } from "@/components/template/simpleSetRepresentability"
import type { SetFormValue } from "@/schemas/template.schema"
import { cn } from "@/lib/utils"
import type { MeasurementType } from "@/types/exercise"

interface SetTableProps {
  name: string
  measurement: MeasurementType
  context: "template" | "routine"
  disabled?: boolean
}

export function SetTable({ name, measurement, context, disabled }: SetTableProps) {
  const { control, getValues } = useFormContext()
  const setsField = useFieldArray({ control, name })

  const simpleAllowed = useWatch({
    control,
    name,
    compute: (sets) => isSimpleRepresentable((sets ?? []) as SetFormValue[], measurement, context),
  })
  const [mode, setMode] = useState<"simple" | "advanced">(() =>
    isSimpleRepresentable((getValues(name) ?? []) as SetFormValue[], measurement, context) ? "simple" : "advanced",
  )
  useEffect(() => {
    if (!simpleAllowed && mode === "simple") setMode("advanced")
  }, [simpleAllowed, mode])
  const activeMode = mode === "simple" && simpleAllowed ? "simple" : "advanced"

  return (
    <div className="space-y-3">
      <div className="inline-flex rounded-md border bg-white p-1">
        <button
          type="button"
          disabled={disabled || !simpleAllowed}
          className={cn("rounded px-3 py-1.5 text-sm", activeMode === "simple" && "bg-primary text-primary-foreground")}
          onClick={() => setMode("simple")}
        >
          Simple
        </button>
        <button
          type="button"
          disabled={disabled}
          className={cn("rounded px-3 py-1.5 text-sm", activeMode === "advanced" && "bg-primary text-primary-foreground")}
          onClick={() => setMode("advanced")}
        >
          Avanzado
        </button>
      </div>

      {!simpleAllowed && <p className="text-sm text-muted-foreground">Estas series usan opciones que el modo Simple no muestra. Editalas en Avanzado para conservar toda la información.</p>}
      <div className={activeMode === "simple" ? "" : "hidden"}>
        <SimpleSetForm name={name} setsField={setsField} measurement={measurement} context={context} disabled={disabled} />
      </div>
      <div className={activeMode === "advanced" ? "" : "hidden"}>
        <AdvancedSetEditor name={name} setsField={setsField} measurement={measurement} context={context} disabled={disabled} />
      </div>
    </div>
  )
}
