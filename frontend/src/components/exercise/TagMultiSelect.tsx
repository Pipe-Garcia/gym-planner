import { groupTags } from "@/components/exercise/TagFilter"
import type { ExerciseTag, TagType } from "@/types/exercise"

const labels: Record<TagType, string> = {
  BODY_AREA: "Zona",
  MUSCLE_GROUP: "Músculo",
  MOVEMENT_PATTERN: "Patrón",
  OBJECTIVE: "Objetivo",
  LEVEL: "Nivel",
  EQUIPMENT: "Equipo",
}

interface TagMultiSelectProps {
  tags: ExerciseTag[]
  value: number[]
  onChange: (ids: number[]) => void
}

export function TagMultiSelect({ tags, value, onChange }: TagMultiSelectProps) {
  const grouped = groupTags(tags)

  function toggle(id: number) {
    onChange(value.includes(id) ? value.filter((item) => item !== id) : [...value, id])
  }

  return (
    <div className="grid items-start gap-3 md:grid-cols-2">
      {Object.entries(grouped).map(([type, items]) => (
        <fieldset key={type} className="self-start rounded-md border bg-white p-3">
          <legend className="px-1 text-sm font-semibold">
            {labels[type as TagType]}{" "}
            <span className="font-normal text-muted-foreground">
              {items.filter((tag) => value.includes(tag.id)).length}/{items.length}
            </span>
          </legend>
          <div className="mt-2 grid max-h-64 gap-1 overflow-y-auto pr-1">
            {items.map((tag) => (
              <label key={tag.id} className="flex min-h-10 items-center gap-2 rounded-md px-2 text-sm hover:bg-muted/70">
                <input type="checkbox" checked={value.includes(tag.id)} onChange={() => toggle(tag.id)} />
                <span>{tag.name}</span>
              </label>
            ))}
          </div>
        </fieldset>
      ))}
    </div>
  )
}
