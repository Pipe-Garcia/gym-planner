import { useCallback, useEffect, useState } from "react"
import type { ReactNode } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { emptySet } from "@/components/template/formDefaults"
import type { MeasurementType } from "@/types/exercise"
import type { ExerciseSetInput } from "@/types/training"

interface Props {
  setsField: { replace: (items: ExerciseSetInput[]) => void }
  sets?: Partial<ExerciseSetInput>[]
  measurement: MeasurementType
  context: "template" | "routine"
  disabled?: boolean
}

export function SimpleSetForm({ setsField, sets = [], measurement, context, disabled }: Props) {
  const { replace } = setsField
  const firstSet = sets[0]
  const [series, setSeries] = useState(measurement === "CIRCUIT_REPS" ? 1 : sets.length > 0 ? sets.length : 3)
  const [reps, setReps] = useState(firstSet?.targetReps ?? (measurement === "TIME" || measurement === "DISTANCE" ? 0 : 10))
  const [time, setTime] = useState(firstSet?.targetTimeSeconds ?? 40)
  const [distance, setDistance] = useState(firstSet?.targetDistanceMeters ?? 100)
  const [weight, setWeight] = useState<number | "">(firstSet?.targetWeightKg ?? "")
  const [rest, setRest] = useState(firstSet?.restAfterSeconds ?? 60)

  const apply = useCallback(() => {
    const count = measurement === "CIRCUIT_REPS" ? 1 : series
    const nextSets = Array.from({ length: count }, (_, index) => {
      const set = emptySet(index + 1)
      set.targetReps = null
      set.targetWeightKg = null
      set.targetTimeSeconds = null
      set.targetDistanceMeters = null
      set.restAfterSeconds = null

      if (measurement === "REPS_WEIGHT" || measurement === "REPS_ONLY" || measurement === "CIRCUIT_REPS") {
        set.targetReps = reps
      }
      if (measurement === "REPS_WEIGHT" && context === "routine") {
        set.targetWeightKg = weight === "" ? null : weight
      }
      if (measurement === "TIME") {
        set.targetTimeSeconds = time
      }
      if (measurement === "DISTANCE") {
        set.targetDistanceMeters = distance
      }
      if (measurement !== "CIRCUIT_REPS") {
        set.restAfterSeconds = rest
      }
      return set
    })
    replace(nextSets)
  }, [context, distance, measurement, replace, reps, rest, series, time, weight])

  useEffect(() => {
    apply()
  }, [apply])

  return (
    <div className="space-y-3 rounded-md border bg-muted/30 p-3">
      <p className="text-sm text-muted-foreground">
        Modo simple: todos los sets serán iguales. Para sets distintos (pirámide, drop set), usá el modo avanzado.
      </p>
      <div className="grid gap-3 sm:grid-cols-[repeat(4,minmax(0,140px))]">
        {measurement !== "CIRCUIT_REPS" ? (
          <Field label="Series">
            <Input type="number" inputMode="numeric" min={1} className="no-spinner w-full sm:max-w-[140px]" value={series} disabled={disabled} onChange={(event) => setSeries(Number(event.target.value))} />
          </Field>
        ) : null}
        {(measurement === "REPS_WEIGHT" || measurement === "REPS_ONLY" || measurement === "CIRCUIT_REPS") ? (
          <Field label="Reps">
            <Input type="number" inputMode="numeric" min={1} className="no-spinner w-full sm:max-w-[140px]" value={reps} disabled={disabled} onChange={(event) => setReps(Number(event.target.value))} />
          </Field>
        ) : null}
        {measurement === "TIME" ? (
          <Field label="Tiempo (seg)">
            <Input type="number" inputMode="numeric" min={1} className="no-spinner w-full sm:max-w-[140px]" value={time} disabled={disabled} onChange={(event) => setTime(Number(event.target.value))} />
          </Field>
        ) : null}
        {measurement === "DISTANCE" ? (
          <Field label="Distancia (m)">
            <Input type="number" inputMode="numeric" min={1} className="no-spinner w-full sm:max-w-[140px]" value={distance} disabled={disabled} onChange={(event) => setDistance(Number(event.target.value))} />
          </Field>
        ) : null}
        {measurement === "REPS_WEIGHT" && context === "routine" ? (
          <Field label="Peso (kg)">
            <Input type="number" inputMode="decimal" min={0} step="0.5" className="no-spinner w-full sm:max-w-[140px]" value={weight} disabled={disabled} onChange={(event) => setWeight(event.target.value === "" ? "" : Number(event.target.value))} />
          </Field>
        ) : null}
        {measurement === "REPS_WEIGHT" && context === "template" ? (
          <div className="self-end pb-2 text-xs italic text-muted-foreground">
            El peso se asigna al crear la rutina del alumno.
          </div>
        ) : null}
        {measurement !== "CIRCUIT_REPS" ? (
          <Field label="Descanso (seg)">
            <Input type="number" inputMode="numeric" min={0} className="no-spinner w-full sm:max-w-[140px]" value={rest} disabled={disabled} onChange={(event) => setRest(Number(event.target.value))} />
          </Field>
        ) : null}
      </div>
      <Button type="button" disabled={disabled} onClick={apply}>Aplicar cambios</Button>
    </div>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return <label className="space-y-1 text-sm font-medium sm:max-w-[140px]">{label}{children}</label>
}
