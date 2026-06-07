import type {
  StudentExerciseOccurrenceSet,
} from "@/types/studentHistory"
import { structuralTypeLabel } from "@/lib/labels"
import type { BlockStructuralType } from "@/types/training"

type StudentExerciseSetsSummaryProps = {
  sets: StudentExerciseOccurrenceSet[]
  structuralType: BlockStructuralType
}

export function StudentExerciseSetsSummary({
  sets,
  structuralType,
}: StudentExerciseSetsSummaryProps) {
  if (sets.length === 0) {
    return (
      <p className="rounded-md bg-slate-50 px-3 py-2 text-sm text-muted-foreground ring-1 ring-slate-100">
        Sin series registradas.
      </p>
    )
  }

  if (isSingleTargetBlock(structuralType)) {
    return <SingleTargetSetsSummary sets={sets} structuralType={structuralType} />
  }

  const first = sets[0]
  const hasExecutionCue = sets.some((set) => normalizedExecutionCue(set.executionCue))
  const allEqual = !hasExecutionCue && sets.every((set) => setSignature(set) === setSignature(first))

  if (allEqual) {
    return (
      <div className="rounded-md bg-slate-50 px-3 py-2.5 text-sm text-slate-800 ring-1 ring-slate-100">
        <p className="leading-6">{sets.length} series × {formatSetParts(first).join(" · ")}</p>
        {first.notes?.trim() ? (
          <p className="mt-1 text-xs text-muted-foreground">
            Nota: {first.notes}
          </p>
        ) : null}
      </div>
    )
  }

  return (
    <div className="grid gap-2 sm:grid-cols-2">
      {sets.map((set) => (
        <SetLine key={set.setNumber} set={set} />
      ))}
    </div>
  )
}

function SingleTargetSetsSummary({ sets, structuralType }: { sets: StudentExerciseOccurrenceSet[]; structuralType: BlockStructuralType }) {
  const label = structuralTypeLabel(structuralType)

  if (sets.length === 1) {
    const set = sets[0]
    return (
      <div className="rounded-md bg-slate-50 px-3 py-2.5 text-sm text-slate-800 ring-1 ring-slate-100">
        <p className="leading-6">{label} · {formatSetParts(set).join(" · ")}</p>
        {set.notes?.trim() ? (
          <p className="mt-1 text-xs text-muted-foreground">
            Nota: {set.notes}
          </p>
        ) : null}
      </div>
    )
  }

  return (
    <div className="rounded-md bg-slate-50 px-3 py-2.5 ring-1 ring-slate-100">
      <p className="text-sm font-medium text-slate-800">{label}</p>
      <div className="mt-2 grid gap-2 sm:grid-cols-2">
        {sets.map((set) => (
          <div key={set.setNumber} className="text-sm leading-6 text-slate-700">
            <p>{formatSetParts(set).join(" · ")}</p>
            {set.notes?.trim() ? (
              <p className="mt-1 text-xs text-muted-foreground">
                Nota: {set.notes}
              </p>
            ) : null}
          </div>
        ))}
      </div>
    </div>
  )
}

function isSingleTargetBlock(structuralType: BlockStructuralType) {
  return structuralType === "CIRCUIT" || structuralType === "GROUPED_SET"
}

function SetLine({ set }: { set: StudentExerciseOccurrenceSet }) {
  return (
    <div className="rounded-md bg-slate-50 px-3 py-2.5 text-sm text-slate-800 ring-1 ring-slate-100">
      <p className="leading-6">Serie {set.setNumber} · {formatSetParts(set).join(" · ")}</p>
      {set.notes?.trim() ? (
        <p className="mt-1 text-xs text-muted-foreground">
          Nota: {set.notes}
        </p>
      ) : null}
    </div>
  )
}

function setSignature(set: StudentExerciseOccurrenceSet) {
  return JSON.stringify({
    targetReps: set.targetReps,
    targetRepsMin: set.targetRepsMin,
    targetRepsMax: set.targetRepsMax,
    targetWeightKg: set.targetWeightKg,
    targetTimeSeconds: set.targetTimeSeconds,
    targetDistanceMeters: set.targetDistanceMeters,
    restAfterSeconds: set.restAfterSeconds,
    rpe: set.rpe,
    tempo: set.tempo,
    toFailure: set.toFailure,
    executionCue: normalizedExecutionCue(set.executionCue),
  })
}

function formatSetParts(set: StudentExerciseOccurrenceSet) {
  const parts: string[] = []

  const reps = formatReps(set)
  if (reps) parts.push(reps)
  if (set.targetWeightKg !== null) parts.push(`${formatNumber(set.targetWeightKg)} kg`)
  if (set.targetTimeSeconds !== null) parts.push(formatSeconds(set.targetTimeSeconds))
  if (set.targetDistanceMeters !== null) {
    parts.push(`${formatNumber(set.targetDistanceMeters)} m`)
  }
  if (set.restAfterSeconds !== null) {
    parts.push(`descanso ${formatSeconds(set.restAfterSeconds)}`)
  }
  if (set.rpe !== null) parts.push(`RPE ${formatNumber(set.rpe)}`)
  if (set.toFailure) parts.push("al fallo")
  if (set.tempo?.trim()) parts.push(`tempo ${set.tempo}`)
  const executionCue = normalizedExecutionCue(set.executionCue)
  if (executionCue) parts.push(executionCue)

  return parts.length > 0 ? parts : ["sin objetivos cargados"]
}

function formatReps(set: StudentExerciseOccurrenceSet) {
  if (set.targetRepsMin !== null && set.targetRepsMax !== null) {
    return `${set.targetRepsMin}-${set.targetRepsMax} reps`
  }
  if (set.targetReps !== null) return `${set.targetReps} reps`
  return null
}

function formatSeconds(seconds: number) {
  if (seconds >= 60) {
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = seconds % 60
    if (remainingSeconds === 0) return `${minutes} min`
    return `${minutes} min ${remainingSeconds} seg`
  }
  return `${seconds} seg`
}

function formatNumber(value: number) {
  return Number.isInteger(value)
    ? String(value)
    : value.toFixed(2).replace(/0+$/, "").replace(/\.$/, "").replace(".", ",")
}

function normalizedExecutionCue(value?: string | null) {
  const trimmed = value?.trim()
  return trimmed ? trimmed : null
}
