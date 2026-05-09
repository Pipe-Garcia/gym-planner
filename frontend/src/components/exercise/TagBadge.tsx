import { cn } from "@/lib/utils"
import type { ExerciseTag, TagType } from "@/types/exercise"

const colors: Record<TagType, string> = {
  BODY_AREA: "bg-blue-100 text-blue-700",
  MUSCLE_GROUP: "bg-indigo-100 text-indigo-700",
  MOVEMENT_PATTERN: "bg-violet-100 text-violet-700",
  OBJECTIVE: "bg-emerald-100 text-emerald-700",
  LEVEL: "bg-amber-100 text-amber-800",
  EQUIPMENT: "bg-slate-100 text-slate-700",
}

export function TagBadge({ tag }: { tag: ExerciseTag }) {
  return <span className={cn("rounded-full px-2.5 py-1 text-xs font-medium", colors[tag.type])}>{tag.name}</span>
}
