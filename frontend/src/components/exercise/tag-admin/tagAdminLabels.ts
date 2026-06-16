import type { TagType } from "@/types/exercise"

export const tagTypeOrder = [
  "BODY_AREA",
  "MUSCLE_GROUP",
  "MOVEMENT_PATTERN",
  "OBJECTIVE",
  "LEVEL",
  "EQUIPMENT",
] as const satisfies readonly TagType[]

export const tagTypeLabels: Record<TagType, string> = {
  BODY_AREA: "Zona corporal",
  MUSCLE_GROUP: "Grupo muscular",
  MOVEMENT_PATTERN: "Patrón de movimiento",
  OBJECTIVE: "Objetivo",
  LEVEL: "Nivel",
  EQUIPMENT: "Equipamiento",
}
