import { ChevronDown, ChevronRight } from "lucide-react"
import { useEffect, useState } from "react"
import { usePreviousLoads } from "@/hooks/usePreviousLoads"
import { formatDateEs } from "@/lib/date"
import { cn } from "@/lib/utils"
import type { MeasurementType } from "@/types/exercise"
import type { PreviousLoadOccurrence, PreviousLoadSet } from "@/types/previousLoads"
import type { BlockPurpose, BlockStructuralType } from "@/types/training"

type PreviousLoadPanelProps = {
  studentId: number
  exerciseId: number
  excludeRoutineId?: number | null
  structuralType?: BlockStructuralType | null
  className?: string
}

export function PreviousLoadPanel({ studentId, exerciseId, excludeRoutineId, structuralType, className }: PreviousLoadPanelProps) {
  const { data, isLoading, isError } = usePreviousLoads({ studentId, exerciseId, excludeRoutineId, structuralType, includeFallback: true, limit: 1 })
  const [expanded, setExpanded] = useState(false)
  const occurrence = data?.occurrences[0]

  useEffect(() => {
    setExpanded(false)
  }, [exerciseId, structuralType])

  if (isLoading) {
    return (
      <div className={cn("rounded-md border bg-muted/30 px-3 py-2 text-xs text-muted-foreground", className)}>
        Cargando cargas previas...
      </div>
    )
  }

  if (isError) {
    return (
      <div className={cn("rounded-md border border-muted bg-muted/20 px-3 py-2 text-xs text-muted-foreground", className)}>
        No se pudieron cargar las cargas previas.
      </div>
    )
  }

  if (!data || !data.found || !occurrence) {
    return (
      <div className={cn("px-1 py-1 text-xs italic text-muted-foreground", className)}>
        Sin cargas previas
      </div>
    )
  }

  const effectiveDate = occurrence.finishedDate ?? occurrence.assignedDate
  const isFallback = data.matchType === "DIFFERENT_STRUCTURAL_TYPE"
  const headerParts = isFallback
    ? [
        "Referencia previa",
        `Ultima carga en ${structuralTypeLabel(occurrence.blockStructuralType)}`,
        effectiveDate ? `Fecha: ${formatDateEs(effectiveDate)}` : null,
        occurrence.routineName ? `Rutina: ${occurrence.routineName}` : null,
      ].filter(Boolean)
    : [
        "Cargas previas",
        effectiveDate ? `Fecha: ${formatDateEs(effectiveDate)}` : null,
        occurrence.routineName ? `Rutina: ${occurrence.routineName}` : null,
        setsSummary(occurrence),
      ].filter(Boolean)
  const contextLine = uniqueContextParts([occurrence.dayName, sectionLabel(occurrence.blockPurpose), occurrence.blockTitle]).join(" · ")
  const formattedSets = formatSets(occurrence)

  return (
    <div
      className={cn(
        "rounded-md border px-3 py-2 text-xs",
        isFallback
          ? "border-slate-200 bg-slate-50 text-slate-900"
          : "border-amber-200 bg-amber-50 text-amber-950",
        className,
      )}
    >
      <button
        type="button"
        className="flex w-full items-start gap-1.5 text-left font-medium"
        onClick={() => setExpanded((value) => !value)}
        aria-expanded={expanded}
      >
        {expanded ? <ChevronDown className="mt-0.5 h-3.5 w-3.5 shrink-0" /> : <ChevronRight className="mt-0.5 h-3.5 w-3.5 shrink-0" />}
        <span className="min-w-0 flex-1 truncate">{headerParts.join(" · ")}</span>
      </button>

      {expanded ? (
        <div className="mt-2 space-y-2 pl-5">
          {isFallback ? (
            <p className="text-[11px] text-slate-700">Referencia de bloque {structuralTypeLabel(occurrence.blockStructuralType)}.</p>
          ) : null}
          {contextLine ? <p className={cn("text-[11px]", isFallback ? "text-slate-700" : "text-amber-900/80")}>{contextLine}</p> : null}
          <div className={cn("grid gap-x-4 gap-y-1", formattedSets.length > 1 ? "sm:grid-cols-2" : "sm:grid-cols-1")}>
            {formattedSets.map((line, index) => (
              <p key={`${occurrence.routineId}-${index}`} className="min-w-0 truncate">
                {line}
              </p>
            ))}
          </div>
          {occurrence.exerciseNotes ? (
            <p className={cn("whitespace-pre-wrap text-[11px]", isFallback ? "text-slate-700" : "text-amber-900/80")}>Nota: {occurrence.exerciseNotes}</p>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}

function sectionLabel(purpose?: BlockPurpose | null) {
  if (!purpose) return null
  if (purpose === "WARMUP" || purpose === "ACTIVATION") return "Calentamiento"
  if (purpose === "COOLDOWN") return "Vuelta a la calma"
  return "Parte principal"
}

function uniqueContextParts(parts: Array<string | null | undefined>) {
  const result: string[] = []

  for (const part of parts) {
    const value = part?.trim()
    if (!value) continue

    const normalized = normalizeForComparison(value)
    const alreadyIncluded = result.some((existing) => normalizeForComparison(existing) === normalized)
    if (!alreadyIncluded) result.push(value)
  }

  return result
}

function normalizeForComparison(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
}

function setsSummary(occurrence: PreviousLoadOccurrence) {
  if (occurrence.blockStructuralType === "CIRCUIT") return "Circuito"
  const count = occurrence.sets.length
  if (count === 1) return "1 serie"
  return `${count} series`
}

function structuralTypeLabel(type: BlockStructuralType) {
  const labels: Record<BlockStructuralType, string> = {
    STANDARD: "Estandar",
    CIRCUIT: "Circuito",
    PYRAMID: "Piramide",
    REVERSE_PYRAMID: "Piramide inversa",
    DROP_SET: "Drop set",
    REST_PAUSE: "Rest pause",
    CLUSTER: "Cluster",
  }
  return labels[type]
}

function formatSets(occurrence: PreviousLoadOccurrence) {
  if (occurrence.blockStructuralType === "CIRCUIT") {
    return [`Circuito · Objetivo ${formatSetTarget(occurrence.sets[0], occurrence.measurementType, { includeRest: false })}`]
  }
  if (occurrence.sets.length === 0) {
    return ["Sin datos de carga"]
  }
  if (allSetsEquivalent(occurrence.sets)) {
    return [`${occurrence.sets.length} series x ${formatSetTarget(occurrence.sets[0], occurrence.measurementType, { restPrefix: true })}`]
  }
  return occurrence.sets.map((set) => `${ordinal(set.setNumber)} · ${formatSetTarget(set, occurrence.measurementType, { restPrefix: true })}`)
}

function formatSetTarget(
  set: PreviousLoadSet | undefined,
  measurementType: MeasurementType,
  options: { includeRest?: boolean; restPrefix?: boolean } = {},
) {
  if (!set) return "Sin datos de carga"

  const parts: string[] = []
  const reps = formatReps(set)
  if (reps) parts.push(reps)
  if (set.toFailure) parts.push("al fallo")
  if (measurementType === "TIME" && set.targetTimeSeconds) parts.push(formatSeconds(set.targetTimeSeconds))
  if (measurementType === "DISTANCE" && set.targetDistanceMeters != null) parts.push(`${formatNumber(set.targetDistanceMeters)} m`)
  if (set.targetWeightKg != null) parts.push(`${formatNumber(set.targetWeightKg)} kg`)
  if (options.includeRest !== false && set.restAfterSeconds) parts.push(formatRest(set.restAfterSeconds, options.restPrefix))
  if (set.rpe != null) parts.push(`RPE ${set.rpe}`)
  const executionCue = normalizedExecutionCue(set.executionCue)
  if (executionCue) parts.push(executionCue)

  return parts.length > 0 ? parts.join(" · ") : "Sin datos de carga"
}

function formatReps(set: PreviousLoadSet) {
  if (set.targetRepsMin != null && set.targetRepsMax != null) {
    return `${set.targetRepsMin}-${set.targetRepsMax} reps`
  }
  if (set.targetReps != null) return `${set.targetReps} reps`
  return null
}

function ordinal(value: number) {
  const labels: Record<number, string> = {
    1: "1ra",
    2: "2da",
    3: "3ra",
    4: "4ta",
    5: "5ta",
  }
  return labels[value] ?? `${value}a`
}

function formatRest(seconds: number, prefixed = false) {
  const value = formatSeconds(seconds)
  return prefixed ? `descanso ${value}` : value
}

function formatSeconds(seconds: number) {
  if (seconds % 60 === 0) {
    const minutes = seconds / 60
    return `${minutes} min`
  }
  return `${seconds}s`
}

function formatNumber(value: number) {
  return Number.isInteger(value) ? String(value) : String(Number(value.toFixed(2)))
}

function allSetsEquivalent(sets: PreviousLoadSet[]) {
  if (sets.length < 2) return false
  if (sets.some((set) => normalizedExecutionCue(set.executionCue))) return false
  const [first, ...rest] = sets
  return rest.every((set) =>
    set.targetReps === first.targetReps &&
    set.targetRepsMin === first.targetRepsMin &&
    set.targetRepsMax === first.targetRepsMax &&
    set.targetWeightKg === first.targetWeightKg &&
    set.targetTimeSeconds === first.targetTimeSeconds &&
    set.targetDistanceMeters === first.targetDistanceMeters &&
    set.restAfterSeconds === first.restAfterSeconds &&
    set.toFailure === first.toFailure,
  )
}

function normalizedExecutionCue(value?: string | null) {
  const trimmed = value?.trim()
  return trimmed ? trimmed : null
}
