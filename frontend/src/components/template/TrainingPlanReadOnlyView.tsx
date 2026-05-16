import { useMemo, useState } from "react"
import { Clock } from "lucide-react"
import { cn } from "@/lib/utils"
import { purposeLabel, structuralTypeLabel } from "@/lib/labels"
import { sectionIconStyleByGroup, sectionStyleByGroup, type SectionGroup } from "@/lib/sectionStyles"
import type { MeasurementType } from "@/types/exercise"
import type { ExerciseInBlock, ExerciseSet, TrainingBlock, TrainingDay } from "@/types/training"

type TrainingContext = "template" | "routine"
type SectionKey = SectionGroup

const sections: { key: SectionKey; title: string }[] = [
  { key: "warmup", title: "Calentamiento" },
  { key: "main", title: "Parte principal" },
  { key: "cooldown", title: "Vuelta a la calma" },
]

interface TrainingPlanReadOnlyViewProps {
  days: TrainingDay[]
  context: TrainingContext
}

export function TrainingPlanReadOnlyView({ days, context }: TrainingPlanReadOnlyViewProps) {
  const [activeIndex, setActiveIndex] = useState(0)
  const orderedDays = useMemo(() => [...days].sort((a, b) => a.orderIndex - b.orderIndex), [days])
  const activeDay = orderedDays[Math.min(activeIndex, Math.max(orderedDays.length - 1, 0))]

  if (!orderedDays.length) {
    return <div className="rounded-md border border-dashed bg-white p-6 text-sm text-muted-foreground">Sin dias cargados.</div>
  }

  return (
    <div className="space-y-4">
      {orderedDays.length > 1 ? (
        <div className="flex gap-2 overflow-x-auto rounded-md border bg-white p-2">
          {orderedDays.map((day, index) => (
            <button
              key={day.id ?? day.orderIndex}
              type="button"
              className={cn(
                "min-h-10 shrink-0 rounded-md px-3 text-sm font-medium",
                index === activeIndex ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted hover:text-foreground",
              )}
              onClick={() => setActiveIndex(index)}
            >
              {day.name || `Dia ${index + 1}`}
            </button>
          ))}
        </div>
      ) : null}

      {activeDay ? <TrainingDayReadOnly day={activeDay} dayNumber={orderedDays.indexOf(activeDay) + 1} context={context} /> : null}
    </div>
  )
}

export function TrainingDayReadOnly({ day, dayNumber, context }: { day: TrainingDay; dayNumber?: number; context: TrainingContext }) {
  const blocks = [...day.blocks].sort((a, b) => a.orderIndex - b.orderIndex)
  return (
    <section className="space-y-4">
      <div>
        <h2 className="text-center text-xl font-semibold tracking-normal">{dayNumber ? `Dia ${dayNumber}: ${day.name}` : day.name}</h2>
        {day.notes ? <p className="mt-1 text-sm text-muted-foreground">{day.notes}</p> : null}
      </div>
      {sections.map((section) => (
        <TrainingSectionReadOnly
          key={section.key}
          group={section.key}
          title={section.title}
          blocks={blocks.filter((block) => sectionOf(block) === section.key)}
          context={context}
        />
      ))}
    </section>
  )
}

export function TrainingSectionReadOnly({ group, title, blocks, context }: { group: SectionGroup; title: string; blocks: TrainingBlock[]; context: TrainingContext }) {
  return (
    <section className={cn("space-y-3 rounded-md border p-4", sectionStyleByGroup(group))}>
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <span className={cn("h-2.5 w-2.5 rounded-full", sectionIconStyleByGroup(group))} />
          <h3 className="font-semibold">{title}</h3>
        </div>
        <span className="text-xs text-muted-foreground">{blocks.length} bloques</span>
      </div>
      {blocks.length ? (
        <div className="space-y-3">
          {blocks.map((block) => <TrainingBlockReadOnly key={block.id ?? block.orderIndex} block={block} context={context} />)}
        </div>
      ) : (
        <div className="rounded-md border border-dashed p-4 text-sm text-muted-foreground">Sin bloques en esta seccion.</div>
      )}
    </section>
  )
}

export function TrainingBlockReadOnly({ block, context }: { block: TrainingBlock; context: TrainingContext }) {
  const isCircuit = block.structuralType === "CIRCUIT"
  return (
    <article className="overflow-hidden rounded-md border">
      <div className="flex flex-col gap-2 border-b bg-muted/30 p-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h4 className="font-medium">{block.title}</h4>
          <p className="text-xs text-muted-foreground">
            {structuralTypeLabel(block.structuralType)} · {purposeLabel(block.purpose)}
          </p>
        </div>
        {isCircuit && block.totalDurationSeconds ? (
          <span className="inline-flex items-center gap-1 rounded-md bg-white px-2 py-1 text-sm text-muted-foreground">
            <Clock className="h-4 w-4" />
            {formatMinutes(block.totalDurationSeconds)}
          </span>
        ) : null}
      </div>
      {block.blockNotes ? <p className="border-b px-3 py-2 text-sm text-muted-foreground">{block.blockNotes}</p> : null}
      <TrainingExerciseTable block={block} context={context} />
    </article>
  )
}

export function TrainingExerciseTable({ block, context }: { block: TrainingBlock; context: TrainingContext }) {
  const isCircuit = block.structuralType === "CIRCUIT"
  const exercises = [...block.exercises].sort((a, b) => a.orderIndex - b.orderIndex)
  const showWeight = context === "routine"

  if (!exercises.length) {
    return <div className="p-4 text-sm text-muted-foreground">Sin ejercicios.</div>
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[720px] text-sm">
        <thead>
          <tr className="border-b bg-muted/20 text-left text-xs uppercase text-muted-foreground">
            <th className="px-3 py-2 font-medium">Ejercicio</th>
            {isCircuit ? null : <th className="px-3 py-2 font-medium">Series</th>}
            <th className="px-3 py-2 font-medium">{isCircuit ? "Objetivo" : "Reps/Tiempo/Distancia"}</th>
            {showWeight ? <th className="px-3 py-2 font-medium">Peso</th> : null}
            {isCircuit ? null : <th className="px-3 py-2 font-medium">Descanso</th>}
            <th className="px-3 py-2 font-medium">Notas</th>
          </tr>
        </thead>
        <tbody>
          {exercises.map((exercise) => (
            <ExerciseTableRow key={exercise.id ?? exercise.orderIndex} exercise={exercise} isCircuit={isCircuit} showWeight={showWeight} />
          ))}
        </tbody>
      </table>
    </div>
  )
}

function ExerciseTableRow({ exercise, isCircuit, showWeight }: { exercise: ExerciseInBlock; isCircuit: boolean; showWeight: boolean }) {
  const sets = [...exercise.sets].sort((a, b) => a.setNumber - b.setNumber)
  const measurement = exercise.defaultMeasurement ?? exercise.exercise?.defaultMeasurement ?? exercise.exerciseMeasurement
  return (
    <tr className="border-b last:border-0">
      <td className="px-3 py-3 align-top">
        <p className="font-medium">{exercise.exerciseName}</p>
        {!exercise.exerciseActive ? <p className="text-xs text-amber-700">Ejercicio inactivo</p> : null}
      </td>
      {isCircuit ? null : <td className="px-3 py-3 align-top">{sets.length || "-"}</td>}
      <td className="px-3 py-3 align-top">{formatTargets(sets, measurement)}</td>
      {showWeight ? <td className="px-3 py-3 align-top">{formatWeights(sets)}</td> : null}
      {isCircuit ? null : <td className="px-3 py-3 align-top">{formatRest(sets)}</td>}
      <td className="px-3 py-3 align-top text-muted-foreground">{formatNotes(exercise, sets)}</td>
    </tr>
  )
}

function sectionOf(block: TrainingBlock): SectionKey {
  if (block.purpose === "WARMUP" || block.purpose === "ACTIVATION") return "warmup"
  if (block.purpose === "COOLDOWN") return "cooldown"
  return "main"
}

function formatMinutes(seconds: number) {
  const minutes = seconds / 60
  return `${Number.isInteger(minutes) ? minutes : minutes.toFixed(1)} min`
}

function formatTargets(sets: ExerciseSet[], measurement: MeasurementType) {
  const values = uniqueValues(sets.map((set) => formatSetTarget(set, measurement)).filter(Boolean))
  return values.length ? values.join(" / ") : "-"
}

function formatSetTarget(set: ExerciseSet, measurement: MeasurementType) {
  if ((measurement === "REPS_WEIGHT" || measurement === "REPS_ONLY" || measurement === "CIRCUIT_REPS") && set.targetReps) return `${set.targetReps} reps`
  if (set.targetRepsMin || set.targetRepsMax) return `${set.targetRepsMin ?? "?"}-${set.targetRepsMax ?? "?"} reps`
  if (measurement === "TIME" && set.targetTimeSeconds) return `${set.targetTimeSeconds}s`
  if (measurement === "DISTANCE" && set.targetDistanceMeters) return `${set.targetDistanceMeters} m`
  return ""
}

function formatWeights(sets: ExerciseSet[]) {
  const values = uniqueValues(sets.map((set) => (set.targetWeightKg != null ? `${set.targetWeightKg} kg` : "")).filter(Boolean))
  return values.length ? values.join(" / ") : "-"
}

function formatRest(sets: ExerciseSet[]) {
  const values = uniqueValues(sets.map((set) => (set.restAfterSeconds != null ? `${set.restAfterSeconds}s` : "")).filter(Boolean))
  return values.length ? values.join(" / ") : "-"
}

function formatNotes(exercise: ExerciseInBlock, sets: ExerciseSet[]) {
  const notes = [exercise.exerciseNotes, ...sets.map((set) => set.notes)].filter((note): note is string => Boolean(note?.trim()))
  return uniqueValues(notes).join(" / ") || "-"
}

function uniqueValues(values: string[]) {
  return Array.from(new Set(values))
}
