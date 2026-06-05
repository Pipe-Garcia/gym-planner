import { Trash2 } from "lucide-react"
import { useFormContext } from "react-hook-form"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { emptySet } from "@/components/template/formDefaults"
import type { MeasurementType } from "@/types/exercise"
import type { ExerciseSetInput } from "@/types/training"

type Column = "reps" | "weight" | "time" | "distance" | "rest" | "notes"
const executionCueSuggestions = ["Completo", "Parcial largo", "Parcial medio", "Parcial corto"]

const nullableNumberRegister = {
  setValueAs: (value: unknown) => {
    if (value === "" || value === undefined || value === null) return null
    const next = Number(value)
    return Number.isNaN(next) ? null : next
  },
}

interface Props {
  name: string
  setsField: {
    fields: { id: string }[]
    append: (item: ExerciseSetInput) => void
    remove: (index: number) => void
  }
  measurement: MeasurementType
  context: "template" | "routine"
  disabled?: boolean
}

export function AdvancedSetEditor({ name, setsField, measurement, context, disabled }: Props) {
  const columns = visibleColumns(measurement, context)

  return (
    <div className="space-y-3">
      <div className="space-y-3">
        {setsField.fields.map((field, index) => (
          <SetEditorRow
            key={field.id}
            name={name}
            index={index}
            columns={columns}
            disabled={disabled}
            onRemove={() => setsField.remove(index)}
          />
        ))}
      </div>

      <Button
        type="button"
        variant="outline"
        size="sm"
        disabled={disabled}
        onClick={() => setsField.append(emptySet(setsField.fields.length + 1))}
      >
        + agregar serie
      </Button>
    </div>
  )
}

function visibleColumns(measurement: MeasurementType, context: "template" | "routine"): Column[] {
  if (measurement === "TIME") return ["time", "rest", "notes"]
  if (measurement === "DISTANCE") return ["distance", "rest", "notes"]
  if (measurement === "CIRCUIT_REPS") return ["reps", "notes"]
  if (measurement === "REPS_ONLY") return ["reps", "rest", "notes"]

  return context === "routine"
    ? ["reps", "weight", "rest", "notes"]
    : ["reps", "rest", "notes"]
}

function SetEditorRow({
  name,
  index,
  columns,
  disabled,
  onRemove,
}: {
  name: string
  index: number
  columns: Column[]
  disabled?: boolean
  onRemove: () => void
}) {
  const { register, setValue } = useFormContext()
  const executionCuePath = `${name}.${index}.executionCue`

  return (
    <div className="rounded-md border bg-white p-3">
      <div className="mb-3 flex items-center justify-between gap-3">
        <p className="font-medium">Serie {index + 1}</p>

        <Button
          type="button"
          size="icon"
          variant="ghost"
          disabled={disabled}
          onClick={onRemove}
          aria-label={`Eliminar serie ${index + 1}`}
        >
          <Trash2 className="h-4 w-4" />
        </Button>
      </div>

      <div className="grid gap-3 sm:grid-cols-[repeat(4,minmax(0,140px))]">
        {columns.includes("reps") ? (
          <label className="space-y-1 text-sm font-medium">
            Reps
            <Input
              type="number"
              inputMode="numeric"
              min={1}
              className="no-spinner w-full"
              disabled={disabled}
              {...register(`${name}.${index}.targetReps`, nullableNumberRegister)}
            />
          </label>
        ) : null}

        {columns.includes("weight") ? (
          <label className="space-y-1 text-sm font-medium">
            Peso (kg)
            <Input
              type="number"
              inputMode="decimal"
              min={0}
              step="0.5"
              className="no-spinner w-full"
              disabled={disabled}
              {...register(`${name}.${index}.targetWeightKg`, nullableNumberRegister)}
            />
          </label>
        ) : null}

        {columns.includes("time") ? (
          <label className="space-y-1 text-sm font-medium">
            Tiempo (seg)
            <Input
              type="number"
              inputMode="numeric"
              min={1}
              className="no-spinner w-full"
              disabled={disabled}
              {...register(`${name}.${index}.targetTimeSeconds`, nullableNumberRegister)}
            />
          </label>
        ) : null}

        {columns.includes("distance") ? (
          <label className="space-y-1 text-sm font-medium">
            Distancia (m)
            <Input
              type="number"
              inputMode="decimal"
              min={0}
              step="0.5"
              className="no-spinner w-full"
              disabled={disabled}
              {...register(`${name}.${index}.targetDistanceMeters`, nullableNumberRegister)}
            />
          </label>
        ) : null}

        {columns.includes("rest") ? (
          <label className="space-y-1 text-sm font-medium">
            Descanso (seg)
            <Input
              type="number"
              inputMode="numeric"
              min={0}
              className="no-spinner w-full"
              disabled={disabled}
              {...register(`${name}.${index}.restAfterSeconds`, nullableNumberRegister)}
            />
          </label>
        ) : null}

        {columns.includes("notes") ? (
          <label className="space-y-1 text-sm font-medium sm:col-span-2">
            Notas
            <Input
              disabled={disabled}
              {...register(`${name}.${index}.notes`)}
            />
          </label>
        ) : null}
      </div>

      <div className="mt-3 space-y-2">
        <label className="space-y-1 text-sm font-medium">
          Indicación
          <Input
            placeholder="Ej: recorrido completo, parcial largo, parcial corto"
            disabled={disabled}
            {...register(executionCuePath)}
          />
        </label>
        <div className="flex flex-wrap gap-2">
          {executionCueSuggestions.map((suggestion) => (
            <Button
              key={suggestion}
              type="button"
              variant="outline"
              size="sm"
              className="h-7 px-2 text-xs"
              disabled={disabled}
              onClick={() => setValue(executionCuePath, suggestion, { shouldDirty: true, shouldTouch: true })}
            >
              {suggestion}
            </Button>
          ))}
        </div>
      </div>
    </div>
  )
}
