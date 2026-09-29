import { useEffect, useState } from "react"
import type { ReactNode } from "react"
import { useFormContext, useWatch } from "react-hook-form"
import { Input } from "@/components/ui/input"
import { emptySet } from "@/components/template/formDefaults"
import type { MeasurementType } from "@/types/exercise"
import type { ExerciseSetInput } from "@/types/training"

type SimpleField = "reps" | "time" | "distance" | "weight" | "rest"
const pathField: Record<SimpleField, keyof ExerciseSetInput> = {
  reps: "targetReps", time: "targetTimeSeconds", distance: "targetDistanceMeters",
  weight: "targetWeightKg", rest: "restAfterSeconds",
}

interface Props {
  name: string
  setsField: {
    fields: { id: string }[]
    append: (items: ExerciseSetInput[]) => void
    remove: (indices: number[]) => void
  }
  measurement: MeasurementType
  context: "template" | "routine"
  disabled?: boolean
}

export function SimpleSetForm({ name, setsField, measurement, context, disabled }: Props) {
  const { control, getValues, setValue } = useFormContext()
  const first = useWatch({ control, name: `${name}.0` }) as ExerciseSetInput | undefined
  const count = setsField.fields.length
  const [seriesInput, setSeriesInput] = useState(() => String(count || 1))
  useEffect(() => setSeriesInput(String(count || 1)), [count])

  function changeField(field: SimpleField, raw: string) {
    if (disabled) return
    const numeric = raw === "" ? null : Number(raw)
    const value = numeric === null || Number.isFinite(numeric) ? numeric : null
    for (let index = 0; index < count; index++) {
      setValue(`${name}.${index}.${pathField[field]}`, value, { shouldDirty: true, shouldTouch: true })
    }
  }

  function commitSeries() {
    if (disabled) return
    const current = (getValues(name) ?? []) as ExerciseSetInput[]
    if (!/^[1-9]\d*$/.test(seriesInput) || !Number.isSafeInteger(Number(seriesInput))) {
      setSeriesInput(String(current.length || 1))
      return
    }
    const target = Number(seriesInput)
    if (target > current.length) {
      const source = current[0]
      const added = Array.from({ length: target - current.length }, (_, offset) => {
        const next = emptySet(current.length + offset + 1)
        if (source) {
          if (["REPS_ONLY", "REPS_WEIGHT", "CIRCUIT_REPS"].includes(measurement)) next.targetReps = source.targetReps
          if (measurement === "REPS_WEIGHT" && context === "routine") next.targetWeightKg = source.targetWeightKg
          if (measurement === "TIME") next.targetTimeSeconds = source.targetTimeSeconds
          if (measurement === "DISTANCE") next.targetDistanceMeters = source.targetDistanceMeters
          if (measurement !== "CIRCUIT_REPS") next.restAfterSeconds = source.restAfterSeconds
        }
        return next
      })
      setsField.append(added)
    } else if (target < current.length) {
      setsField.remove(Array.from({ length: current.length - target }, (_, offset) => target + offset))
    }
    setSeriesInput(String(target))
  }

  const inputValue = (value: number | null | undefined) => value ?? ""
  const numericInput = (label: string, field: SimpleField, value: number | null | undefined, inputMode: "numeric" | "decimal" = "numeric", min = 1) => (
    <Field label={label}>
      <Input type="number" inputMode={inputMode} min={min} className="no-spinner w-full sm:max-w-[140px]"
        value={inputValue(value)} disabled={disabled} onChange={(event) => changeField(field, event.target.value)} />
    </Field>
  )

  return (
    <div className="space-y-3 rounded-md border bg-muted/30 p-3">
      <p className="text-sm text-muted-foreground">Modo simple: todas las series serán iguales. Para series distintas, usá el modo avanzado.</p>
      <div className="grid gap-3 sm:grid-cols-[repeat(4,minmax(0,140px))]">
        {measurement !== "CIRCUIT_REPS" && <Field label="Series">
          <Input type="number" inputMode="numeric" min={1} className="no-spinner w-full sm:max-w-[140px]"
            value={seriesInput} disabled={disabled} onChange={(event) => setSeriesInput(event.target.value)}
            onBlur={commitSeries} onKeyDown={(event) => { if (event.key === "Enter") { event.preventDefault(); commitSeries() } }} />
        </Field>}
        {["REPS_WEIGHT", "REPS_ONLY", "CIRCUIT_REPS"].includes(measurement) && numericInput("Reps", "reps", first?.targetReps)}
        {measurement === "TIME" && numericInput("Tiempo (seg)", "time", first?.targetTimeSeconds)}
        {measurement === "DISTANCE" && numericInput("Distancia (m)", "distance", first?.targetDistanceMeters)}
        {measurement === "REPS_WEIGHT" && context === "routine" && numericInput("Peso (kg)", "weight", first?.targetWeightKg, "decimal", 0)}
        {measurement === "REPS_WEIGHT" && context === "template" && <div className="self-end pb-2 text-xs italic text-muted-foreground">El peso se asigna al crear la rutina del alumno.</div>}
        {measurement !== "CIRCUIT_REPS" && numericInput("Descanso (seg)", "rest", first?.restAfterSeconds, "numeric", 0)}
      </div>
    </div>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return <label className="space-y-1 text-sm font-medium sm:max-w-[140px]">{label}{children}</label>
}
