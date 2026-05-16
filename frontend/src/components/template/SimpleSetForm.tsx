import { useEffect, useMemo, useRef } from "react"
import type { ReactNode } from "react"
import { Input } from "@/components/ui/input"
import { emptySet } from "@/components/template/formDefaults"
import type { MeasurementType } from "@/types/exercise"
import type { ExerciseSetInput } from "@/types/training"

interface Props {
  setsField: {
    replace: (items: ExerciseSetInput[]) => void
    fields: { id: string }[]
  }
  sets?: Partial<ExerciseSetInput>[]
  measurement: MeasurementType
  context: "template" | "routine"
  disabled?: boolean
  active?: boolean
}

/**
 * Modo simple: todos los sets son iguales. Cualquier cambio se aplica
 * inmediatamente al form state — no hay botón "Aplicar".
 *
 * Si el usuario quiere sets distintos (pirámide, drop set, etc.), debe
 * cambiar al modo Avanzado.
 */
export function SimpleSetForm({
  setsField,
  sets = [],
  measurement,
  context,
  disabled,
  active = true,
}: Props) {
  const { replace } = setsField
  const firstSet = sets[0]

  // Valores derivados de los sets actuales. No usamos useState local porque
  // queremos que el form sea la única fuente de verdad.
  const values = useMemo(
    () => ({
      series:
        measurement === "CIRCUIT_REPS"
          ? 1
          : sets.length > 0
          ? sets.length
          : 3,
      reps: firstSet?.targetReps ?? null,
      time: firstSet?.targetTimeSeconds ?? null,
      distance: firstSet?.targetDistanceMeters ?? null,
      weight: firstSet?.targetWeightKg ?? null,
      rest: firstSet?.restAfterSeconds ?? null,
    }),
    [sets, measurement, firstSet],
  )

  // Cuando se monta por primera vez con sets vacíos, inicializa con un set
  // vacío para que el modo simple tenga estructura. Solo una vez.
  const initialized = useRef(false)
  useEffect(() => {
    if (!active || initialized.current) return
    if (sets.length === 0) {
      replace([emptySet(1)])
    }
    initialized.current = true
  }, [active, sets.length, replace])

  function update(field: keyof typeof values, value: number | null) {
    const count =
      field === "series"
        ? Math.max(1, value ?? 1)
        : measurement === "CIRCUIT_REPS"
        ? 1
        : values.series

    const nextValues = { ...values, [field]: value }

    const nextSets: ExerciseSetInput[] = Array.from(
      { length: count },
      (_, index) => {
        const set = emptySet(index + 1)
        set.targetReps = null
        set.targetWeightKg = null
        set.targetTimeSeconds = null
        set.targetDistanceMeters = null
        set.restAfterSeconds = null

        if (
          measurement === "REPS_WEIGHT" ||
          measurement === "REPS_ONLY" ||
          measurement === "CIRCUIT_REPS"
        ) {
          set.targetReps = nextValues.reps
        }
        if (measurement === "REPS_WEIGHT" && context === "routine") {
          set.targetWeightKg = nextValues.weight
        }
        if (measurement === "TIME") {
          set.targetTimeSeconds = nextValues.time
        }
        if (measurement === "DISTANCE") {
          set.targetDistanceMeters = nextValues.distance
        }
        if (measurement !== "CIRCUIT_REPS") {
          set.restAfterSeconds = nextValues.rest
        }
        return set
      },
    )

    replace(nextSets)
  }

  function inputValue(v: number | null) {
    return v === null ? "" : v
  }

  function parseNumber(raw: string): number | null {
    if (raw === "") return null
    const n = Number(raw)
    return Number.isNaN(n) ? null : n
  }

  return (
    <div className="space-y-3 rounded-md border bg-muted/30 p-3">
      <p className="text-sm text-muted-foreground">
        Modo simple: todas las series serán iguales. Para series distintas
        (pirámide, drop set), usá el modo avanzado.
      </p>
      <div className="grid gap-3 sm:grid-cols-[repeat(4,minmax(0,140px))]">
        {measurement !== "CIRCUIT_REPS" && (
          <Field label="Series">
            <Input
              type="number"
              inputMode="numeric"
              min={1}
              className="no-spinner w-full sm:max-w-[140px]"
              value={inputValue(values.series)}
              disabled={disabled}
              onChange={(e) => update("series", parseNumber(e.target.value) ?? 1)}
            />
          </Field>
        )}
        {(measurement === "REPS_WEIGHT" ||
          measurement === "REPS_ONLY" ||
          measurement === "CIRCUIT_REPS") && (
          <Field label="Reps">
            <Input
              type="number"
              inputMode="numeric"
              min={1}
              className="no-spinner w-full sm:max-w-[140px]"
              value={inputValue(values.reps)}
              disabled={disabled}
              onChange={(e) => update("reps", parseNumber(e.target.value))}
            />
          </Field>
        )}
        {measurement === "TIME" && (
          <Field label="Tiempo (seg)">
            <Input
              type="number"
              inputMode="numeric"
              min={1}
              className="no-spinner w-full sm:max-w-[140px]"
              value={inputValue(values.time)}
              disabled={disabled}
              onChange={(e) => update("time", parseNumber(e.target.value))}
            />
          </Field>
        )}
        {measurement === "DISTANCE" && (
          <Field label="Distancia (m)">
            <Input
              type="number"
              inputMode="numeric"
              min={1}
              className="no-spinner w-full sm:max-w-[140px]"
              value={inputValue(values.distance)}
              disabled={disabled}
              onChange={(e) => update("distance", parseNumber(e.target.value))}
            />
          </Field>
        )}
        {measurement === "REPS_WEIGHT" && context === "routine" && (
          <Field label="Peso (kg)">
            <Input
              type="number"
              inputMode="decimal"
              min={0}
              step="0.5"
              className="no-spinner w-full sm:max-w-[140px]"
              value={inputValue(values.weight)}
              disabled={disabled}
              onChange={(e) => update("weight", parseNumber(e.target.value))}
            />
          </Field>
        )}
        {measurement === "REPS_WEIGHT" && context === "template" && (
          <div className="self-end pb-2 text-xs italic text-muted-foreground">
            El peso se asigna al crear la rutina del alumno.
          </div>
        )}
        {measurement !== "CIRCUIT_REPS" && (
          <Field label="Descanso (seg)">
            <Input
              type="number"
              inputMode="numeric"
              min={0}
              className="no-spinner w-full sm:max-w-[140px]"
              value={inputValue(values.rest)}
              disabled={disabled}
              onChange={(e) => update("rest", parseNumber(e.target.value))}
            />
          </Field>
        )}
      </div>
    </div>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="space-y-1 text-sm font-medium sm:max-w-[140px]">
      {label}
      {children}
    </label>
  )
}