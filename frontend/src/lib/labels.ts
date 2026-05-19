import type { BlockPurpose, BlockStructuralType, RoutineStatus, SetKind } from "@/types/training"
import { cn } from "@/lib/utils"

export function structuralTypeLabel(type?: BlockStructuralType | null) {
  const labels: Record<BlockStructuralType, string> = {
    STANDARD: "Estándar",
    CIRCUIT: "Circuito",
    PYRAMID: "Pirámide",
    REVERSE_PYRAMID: "Pirámide inversa",
    DROP_SET: "Drop set",
    REST_PAUSE: "Rest pause",
    CLUSTER: "Cluster",
  }
  return type ? labels[type] : "-"
}

export function purposeLabel(purpose?: BlockPurpose | null) {
  const labels: Record<BlockPurpose, string> = {
    WARMUP: "Calentamiento",
    ACTIVATION: "Activación",
    MAIN_LIFT: "Parte principal",
    ACCESSORY: "Accesorio",
    CONDITIONING: "Acondicionamiento",
    CORE: "Core",
    COOLDOWN: "Vuelta a la calma",
    OTHER: "Otro",
  }
  return purpose ? labels[purpose] : "Sin propósito"
}

export function setKindLabel(kind?: SetKind | null) {
  const labels: Record<SetKind, string> = {
    NORMAL: "Normal",
    WARMUP: "Calentamiento",
    FAILURE: "Al fallo",
    DROP: "Drop",
    REST_PAUSE_PORTION: "Rest pause",
  }
  return kind ? labels[kind] : "Normal"
}

export function routineStatusLabel(status?: RoutineStatus | "CANCELLED" | null) {
  const labels: Record<RoutineStatus | "CANCELLED", string> = {
    ACTIVE: "Activa",
    FINISHED: "Finalizada",
    ARCHIVED: "Archivada",
    DRAFT: "Borrador",
    CANCELLED: "Cancelada",
  }
  return status ? labels[status] : "-"
}

export function routineStatusBadgeClass(status?: RoutineStatus | "CANCELLED" | null) {
  const styles: Record<RoutineStatus | "CANCELLED", string> = {
    ACTIVE: "bg-emerald-50 text-emerald-700 ring-emerald-200",
    DRAFT: "bg-amber-50 text-amber-800 ring-amber-200",
    FINISHED: "bg-sky-50 text-sky-700 ring-sky-200",
    ARCHIVED: "bg-slate-100 text-slate-700 ring-slate-300",
    CANCELLED: "bg-rose-50 text-rose-700 ring-rose-200",
  }
  return cn(
    "inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium ring-1 ring-inset",
    status ? styles[status] : "bg-muted text-muted-foreground ring-border",
  )
}
