import { Button } from "@/components/ui/button"
import type { ExerciseTag, TagType } from "@/types/exercise"

const labels: Record<TagType, string> = {
  BODY_AREA: "Zona",
  MUSCLE_GROUP: "Músculo",
  MOVEMENT_PATTERN: "Patrón",
  OBJECTIVE: "Objetivo",
  LEVEL: "Nivel",
  EQUIPMENT: "Equipo",
}

interface TagFilterProps {
  tags: ExerciseTag[]
  selectedIds: number[]
  onChange: (ids: number[]) => void
}

export function TagFilter({ tags, selectedIds, onChange }: TagFilterProps) {
  const grouped = groupTags(tags)

  function toggle(id: number) {
    onChange(selectedIds.includes(id) ? selectedIds.filter((item) => item !== id) : [...selectedIds, id])
  }

  return (
    <div className="space-y-3">
      {selectedIds.length > 0 ? (
        <div className="flex flex-wrap gap-2 rounded-md border bg-white p-3">
          {tags
            .filter((tag) => selectedIds.includes(tag.id))
            .map((tag) => (
              <button
                key={tag.id}
                type="button"
                onClick={() => toggle(tag.id)}
                className="rounded-full bg-primary/10 px-2.5 py-1 text-xs font-medium text-primary"
              >
                {tag.name} x
              </button>
            ))}
        </div>
      ) : null}
      <div className="max-h-[calc(100vh-15rem)] space-y-3 overflow-y-auto pr-1">
        {Object.entries(grouped).map(([type, items]) => {
          const selectedCount = items.filter((tag) => selectedIds.includes(tag.id)).length
          return (
            <details key={type} open className="rounded-md border bg-white p-3">
              <summary className="flex cursor-pointer list-none items-center justify-between gap-3 text-sm font-semibold">
                <span>{labels[type as TagType]}</span>
                <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
                  {selectedCount}/{items.length}
                </span>
              </summary>
              <div className="mt-3 grid max-h-56 gap-1 overflow-y-auto pr-1">
                {items.map((tag) => (
                  <label key={tag.id} className="flex min-h-10 items-center gap-2 rounded-md px-2 text-sm hover:bg-muted/70">
                    <input type="checkbox" checked={selectedIds.includes(tag.id)} onChange={() => toggle(tag.id)} />
                    <span>{tag.name}</span>
                  </label>
                ))}
              </div>
            </details>
          )
        })}
      </div>
      <Button type="button" variant="outline" className="w-full" onClick={() => onChange([])}>
        Limpiar filtros
      </Button>
    </div>
  )
}

export function groupTags(tags: ExerciseTag[]) {
  const order: TagType[] = ["BODY_AREA", "MUSCLE_GROUP", "MOVEMENT_PATTERN", "OBJECTIVE", "LEVEL", "EQUIPMENT"]
  const grouped = tags.reduce<Record<string, ExerciseTag[]>>((acc, tag) => {
    acc[tag.type] = acc[tag.type] ?? []
    acc[tag.type].push(tag)
    return acc
  }, {})

  return order.reduce<Record<string, ExerciseTag[]>>((acc, type) => {
    if (grouped[type]?.length) {
      acc[type] = grouped[type]
    }
    return acc
  }, {})
}
