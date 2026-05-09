import { format } from "date-fns"
import { es } from "date-fns/locale"
import type { MeasurementType } from "@/types/exercise"

export function formatDateTime(value: string | Date) {
  return format(new Date(value), "dd/MM/yyyy HH:mm", { locale: es })
}

export const measurementTypeOptions: Array<{ value: MeasurementType; label: string }> = [
  { value: "REPS_WEIGHT", label: "Reps + peso" },
  { value: "REPS_ONLY", label: "Solo reps" },
  { value: "TIME", label: "Tiempo" },
  { value: "DISTANCE", label: "Distancia" },
  { value: "CIRCUIT_REPS", label: "Circuito / reps" },
]

export function measurementTypeLabel(value: MeasurementType) {
  return measurementTypeOptions.find((option) => option.value === value)?.label ?? value
}
