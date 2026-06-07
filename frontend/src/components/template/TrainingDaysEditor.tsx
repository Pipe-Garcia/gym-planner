import { Copy, Plus, Trash2 } from "lucide-react"
import { useMemo, useState } from "react"
import { useFieldArray, useFormContext, useWatch } from "react-hook-form"
import { TrainingSectionsEditor } from "@/components/template/TrainingSectionsEditor"
import { defaultDay, normalizeBlockOrder } from "@/components/template/formDefaults"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"
import type { DayInput } from "@/types/training"

export function TrainingDaysEditor({
  context = "template",
  disabled = false,
  studentId,
  excludeRoutineId,
}: {
  context?: "template" | "routine"
  disabled?: boolean
  studentId?: number
  excludeRoutineId?: number | null
}) {
  const { control, register } = useFormContext()
  const days = useFieldArray({ control, name: "days" })
  const watched = useWatch({ control, name: "days" })
  const watchedDays = useMemo<DayInput[]>(
    () => (watched ?? []) as DayInput[],
    [watched],
  )
  const [selected, setSelected] = useState(0)
  const activeIndex = Math.min(selected, Math.max(days.fields.length - 1, 0))
  const singleDay = days.fields.length <= 1

  const activeName = useMemo(() => watchedDays[activeIndex]?.name?.trim() || `Dia ${activeIndex + 1}`, [activeIndex, watchedDays])

  function addDay() {
    const index = days.fields.length + 1
    days.append(defaultDay(index))
    setSelected(index - 1)
  }

  function duplicateDay(index: number) {
    const source = watchedDays[index]
    if (!source) return
    const copy = cloneDay(source, days.fields.length + 1)
    days.append(copy)
    setSelected(days.fields.length)
  }

  function removeDay(index: number) {
    if (days.fields.length <= 1) return
    days.remove(index)
    setSelected(Math.max(0, index - 1))
  }

  return (
    <div className="space-y-4">
      {!singleDay ? (
        <div className="flex flex-wrap items-center gap-2 border-b pb-2">
          {days.fields.map((field, index) => (
            <button key={field.id} type="button" className={cn("rounded-md px-3 py-2 text-sm", index === activeIndex ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground")} onClick={() => setSelected(index)}>
              {watchedDays[index]?.name?.trim() || `Dia ${index + 1}`}
            </button>
          ))}
          {!disabled ? (
            <Button type="button" variant="outline" size="sm" onClick={addDay}>
              <Plus className="h-4 w-4" />
              Agregar dia
            </Button>
          ) : null}
        </div>
      ) : null}

      {singleDay && !disabled ? (
        <div className="flex justify-end">
          <Button type="button" variant="outline" onClick={addDay}>
            <Plus className="h-4 w-4" />
            Agregar dia
          </Button>
        </div>
      ) : null}

      <section className="space-y-4">
        <div className="grid gap-3 rounded-md border bg-white p-4 sm:grid-cols-[1fr_auto_auto]">
          <label className="space-y-1 text-sm font-medium">
            Dia
            <Input disabled={disabled} {...register(`days.${activeIndex}.name`)} />
          </label>
          {!disabled ? (
            <Button type="button" variant="outline" className="self-end" onClick={() => duplicateDay(activeIndex)}>
              <Copy className="h-4 w-4" />
              Duplicar dia
            </Button>
          ) : null}
          {!disabled && !singleDay ? (
            <Button type="button" variant="ghost" size="icon" className="self-end" aria-label={`Eliminar ${activeName}`} onClick={() => removeDay(activeIndex)}>
              <Trash2 className="h-4 w-4" />
            </Button>
          ) : null}
        </div>
        <TrainingSectionsEditor
          key={activeIndex}
          dayIndex={activeIndex}
          context={context}
          disabled={disabled}
          studentId={studentId}
          excludeRoutineId={excludeRoutineId}
        />
      </section>
    </div>
  )
}

function cloneDay(day: DayInput, orderIndex: number): DayInput {
  return {
    orderIndex,
    name: buildCopyName(day.name, orderIndex),
    notes: day.notes ?? null,
    blocks: normalizeBlockOrder(day.blocks ?? []).map((block) => ({
      orderIndex: block.orderIndex,
      title: block.title,
      structuralType: block.structuralType,
      purpose: block.purpose,
      totalDurationSeconds: block.totalDurationSeconds,
      targetRounds: block.targetRounds,
      roundRestSeconds: block.roundRestSeconds,
      blockNotes: block.blockNotes,
      exercises: normalizeBlockOrder(block.exercises ?? []).map((exercise) => ({
        exerciseId: exercise.exerciseId,
        exerciseName: exercise.exerciseName,
        exerciseMeasurement: exercise.exerciseMeasurement,
        orderIndex: exercise.orderIndex,
        exerciseNotes: exercise.exerciseNotes,
        sets: (exercise.sets ?? []).map((set, setIndex) => ({
          setNumber: setIndex + 1,
          setKind: set.setKind,
          targetReps: set.targetReps,
          targetRepsMin: set.targetRepsMin,
          targetRepsMax: set.targetRepsMax,
          targetWeightKg: set.targetWeightKg,
          targetTimeSeconds: set.targetTimeSeconds,
          targetDistanceMeters: set.targetDistanceMeters,
          restAfterSeconds: set.restAfterSeconds,
          tempo: set.tempo,
          executionCue: set.executionCue ?? null,
          rpe: set.rpe,
          notes: set.notes,
          toFailure: set.toFailure,
        })),
      })),
    })),
  }
}

function buildCopyName(originalName: string | undefined, orderIndex: number): string {
  const fallback = `Dia ${orderIndex}`
  const base = (originalName || fallback).replace(/\s*\(copia(?:\s\d+)?\)$/i, "").trim()
  return `${base} (copia)`.trim()
}
