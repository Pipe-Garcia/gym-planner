import { Trash2 } from "lucide-react"
import { useEffect } from "react"
import { useFormContext, useWatch } from "react-hook-form"
import { ReorderButtons } from "@/components/template/ReorderButtons"
import { SetTable } from "@/components/template/SetTable"
import { emptySet } from "@/components/template/formDefaults"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import type { MeasurementType } from "@/types/exercise"
import type { BlockStructuralType } from "@/types/training"

const nullableNumberRegister = {
  setValueAs: (value: unknown) => {
    if (value === "" || value === undefined || value === null) return null
    const next = Number(value)
    return Number.isNaN(next) ? null : next
  },
}

interface Props {
  blockPath: string
  exerciseIndex: number
  exercisesLength: number
  onRemove: () => void
  onMoveUp: () => void
  onMoveDown: () => void
  disabled?: boolean
  context?: "template" | "routine"
}

export function ExerciseInBlockRow({ blockPath, exerciseIndex, exercisesLength, onRemove, onMoveUp, onMoveDown, disabled, context = "template" }: Props) {
  const { control, register, watch } = useFormContext()
  const prefix = `${blockPath}.exercises.${exerciseIndex}`
  const name = watch(`${prefix}.exerciseName`) as string | undefined
  const measurement = (useWatch({ control, name: `${prefix}.exerciseMeasurement` }) ?? "REPS_WEIGHT") as MeasurementType
  const structuralType = useWatch({ control, name: `${blockPath}.structuralType` }) as BlockStructuralType | undefined

  return (
    <div className="rounded-md border bg-white p-3">
      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="flex shrink-0 gap-1 sm:flex-col">
          <ReorderButtons onMoveUp={onMoveUp} onMoveDown={onMoveDown} disableUp={exerciseIndex === 0} disableDown={exerciseIndex === exercisesLength - 1} disabled={disabled} orientation="responsive" />
        </div>
        <div className="min-w-0 flex-1 space-y-3">
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0 flex-1">
              <p className="truncate font-medium">{name || "Ejercicio"}</p>
              <p className="text-xs text-muted-foreground">Orden {exerciseIndex + 1}</p>
            </div>
            <Button type="button" size="icon" variant="ghost" disabled={disabled} onClick={onRemove} aria-label="Quitar ejercicio">
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
          <Input placeholder="Notas del ejercicio" disabled={disabled} {...register(`${prefix}.exerciseNotes`)} />
          {structuralType === "CIRCUIT" ? (
            <CircuitExerciseRow prefix={prefix} measurement={measurement} context={context} disabled={disabled} />
          ) : (
            <SetTable name={`${prefix}.sets`} measurement={measurement} context={context} disabled={disabled} />
          )}
        </div>
      </div>
    </div>
  )
}

export function CircuitExerciseRow({ prefix, measurement, context, disabled }: { prefix: string; measurement: MeasurementType; context: "template" | "routine"; disabled?: boolean }) {
  const { register, setValue, getValues } = useFormContext()
  const setPath = `${prefix}.sets.0`

  useEffect(() => {
    const sets = getValues(`${prefix}.sets`)
    if (!Array.isArray(sets) || sets.length === 0) {
      setValue(`${prefix}.sets`, [emptySet(1)], { shouldDirty: true })
    }
  }, [getValues, prefix, setValue])

  if (measurement === "TIME") {
    return (
      <label className="block space-y-1 text-sm font-medium sm:max-w-[160px]">
        Tiempo de trabajo (seg)
        <Input type="number" inputMode="numeric" min="1" className="no-spinner w-full sm:max-w-[160px]" disabled={disabled} {...register(`${setPath}.targetTimeSeconds`, nullableNumberRegister)} />
      </label>
    )
  }

  if (measurement === "DISTANCE") {
    return (
      <label className="block space-y-1 text-sm font-medium sm:max-w-[160px]">
        Distancia (m)
        <Input type="number" inputMode="decimal" min="1" className="no-spinner w-full sm:max-w-[160px]" disabled={disabled} {...register(`${setPath}.targetDistanceMeters`, nullableNumberRegister)} />
      </label>
    )
  }

  return (
    <div className={context === "routine" && measurement === "REPS_WEIGHT" ? "grid gap-3 sm:grid-cols-[minmax(0,160px)_minmax(0,160px)]" : "grid gap-3 sm:grid-cols-[minmax(0,160px)]"}>
      <label className="block space-y-1 text-sm font-medium sm:max-w-[160px]">
        Reps objetivo
        <Input type="number" inputMode="numeric" min="1" className="no-spinner w-full sm:max-w-[160px]" disabled={disabled} {...register(`${setPath}.targetReps`, nullableNumberRegister)} />
      </label>
      {context === "routine" && measurement === "REPS_WEIGHT" ? (
        <label className="block space-y-1 text-sm font-medium sm:max-w-[160px]">
          Peso objetivo (kg)
          <Input type="number" inputMode="decimal" min="0" step="0.5" className="no-spinner w-full sm:max-w-[160px]" disabled={disabled} {...register(`${setPath}.targetWeightKg`, nullableNumberRegister)} />
        </label>
      ) : null}
    </div>
  )
}
