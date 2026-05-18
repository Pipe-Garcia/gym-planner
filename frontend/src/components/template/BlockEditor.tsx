import { Plus, Trash2 } from "lucide-react"
import { useEffect, useState } from "react"
import { Controller, useFieldArray, useFormContext, useWatch } from "react-hook-form"
import { BlockTypeSelector } from "@/components/template/BlockTypeSelector"
import { ExerciseInBlockRow } from "@/components/template/ExerciseInBlockRow"
import { ExercisePicker } from "@/components/template/ExercisePicker"
import { ReorderButtons } from "@/components/template/ReorderButtons"
import { emptySet } from "@/components/template/formDefaults"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"
import type { ExerciseSummary } from "@/types/exercise"
import type { BlockStructuralType } from "@/types/training"

interface Props {
  blockIndex: number
  blockPath: string
  blocksLength: number
  onRemove: () => void
  onMoveUp: () => void
  onMoveDown: () => void
  disabled?: boolean
  disableUp?: boolean
  disableDown?: boolean
  context?: "template" | "routine"
  studentId?: number
  excludeRoutineId?: number | null
}

export function BlockEditor({ blockIndex, blockPath, blocksLength, onRemove, onMoveUp, onMoveDown, disabled, disableUp, disableDown, context = "template", studentId, excludeRoutineId }: Props) {
  const { control, register, setValue } = useFormContext()
  const [pickerOpen, setPickerOpen] = useState(false)
  const exercises = useFieldArray({ control, name: `${blockPath}.exercises` })
  const structuralType = useWatch({ control, name: `${blockPath}.structuralType` }) as BlockStructuralType | undefined

  useEffect(() => {
    if (structuralType !== "CIRCUIT") {
      setValue(`${blockPath}.totalDurationSeconds`, null, { shouldDirty: true })
    }
    setValue(`${blockPath}.targetRounds`, null, { shouldDirty: true })
  }, [blockPath, setValue, structuralType])

  function addExercise(exercise: ExerciseSummary) {
    exercises.append({ exerciseId: exercise.id, exerciseName: exercise.name, exerciseMeasurement: exercise.defaultMeasurement, orderIndex: exercises.fields.length + 1, exerciseNotes: null, sets: [emptySet(1)] })
  }

  return (
    <Card>
      <CardContent className="space-y-4 p-4 sm:p-6">
        <div className="flex flex-col gap-3 sm:flex-row">
          <div className="shrink-0">
            <ReorderButtons onMoveUp={onMoveUp} onMoveDown={onMoveDown} disableUp={disableUp ?? blockIndex === 0} disableDown={disableDown ?? blockIndex === blocksLength - 1} disabled={disabled} orientation="responsive" />
          </div>
          <div className="min-w-0 flex-1 space-y-4">
            <div
              className={cn(
                "grid gap-3",
                structuralType === "CIRCUIT"
                  ? "sm:grid-cols-[minmax(0,2fr)_minmax(0,1fr)_minmax(0,1fr)_auto] sm:items-end"
                  : "sm:grid-cols-[minmax(0,1fr)_minmax(0,220px)_auto] sm:items-end",
              )}
            >
              <label className="space-y-1 text-sm font-medium">
                Titulo del bloque
                <Input placeholder="Titulo del bloque" disabled={disabled} {...register(`${blockPath}.title`)} />
              </label>

              <div className="space-y-1 text-sm font-medium">
                <span>Tipo de bloque</span>
                <BlockTypeSelector name={`${blockPath}.structuralType`} disabled={disabled} />
              </div>

              {structuralType === "CIRCUIT" ? (
                <label className="space-y-1 text-sm font-medium">
                  Duracion (minutos)
                  <Controller
                    control={control}
                    name={`${blockPath}.totalDurationSeconds`}
                    render={({ field }) => (
                      <Input
                        type="number"
                        inputMode="decimal"
                        step="0.5"
                        min="0.5"
                        placeholder="ej: 12"
                        className="no-spinner"
                        disabled={disabled}
                        value={field.value == null ? "" : Number((Number(field.value) / 60).toFixed(2))}
                        onChange={(event) => {
                          const minutes = event.target.value === "" ? null : Number(event.target.value)
                          field.onChange(minutes == null || Number.isNaN(minutes) ? null : Math.round(minutes * 60))
                        }}
                      />
                    )}
                  />
                </label>
              ) : null}

              <Button type="button" size="icon" variant="ghost" className="self-end" disabled={disabled} onClick={onRemove} aria-label="Eliminar bloque">
                <Trash2 className="h-4 w-4" />
              </Button>
            </div>

            <div className="space-y-3">
              {exercises.fields.map((field, exerciseIndex) => (
                <ExerciseInBlockRow
                  key={field.id}
                  blockPath={blockPath}
                  exerciseIndex={exerciseIndex}
                  exercisesLength={exercises.fields.length}
                  onRemove={() => exercises.remove(exerciseIndex)}
                  onMoveUp={() => exerciseIndex > 0 && exercises.swap(exerciseIndex, exerciseIndex - 1)}
                  onMoveDown={() => exerciseIndex < exercises.fields.length - 1 && exercises.swap(exerciseIndex, exerciseIndex + 1)}
                  disabled={disabled}
                  context={context}
                  studentId={studentId}
                  excludeRoutineId={excludeRoutineId}
                  structuralType={structuralType}
                />
              ))}
              <Button type="button" variant="outline" disabled={disabled} onClick={() => setPickerOpen(true)}>
                <Plus className="h-4 w-4" />
                Agregar ejercicio
              </Button>
            </div>
          </div>
        </div>
      </CardContent>
      <ExercisePicker open={pickerOpen} onOpenChange={setPickerOpen} onSelect={addExercise} />
    </Card>
  )
}
