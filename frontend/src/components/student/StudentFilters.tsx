import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

interface StudentFiltersProps {
  active?: boolean
  sport: string
  level: string
  onActiveChange: (active?: boolean) => void
  onSportChange: (sport: string) => void
  onLevelChange: (level: string) => void
  onClear: () => void
}

export function StudentFilters({
  active,
  sport,
  level,
  onActiveChange,
  onSportChange,
  onLevelChange,
  onClear,
}: StudentFiltersProps) {
  return (
    <div className="space-y-4">
      <label className="space-y-2 text-sm font-medium">
        Estado
        <select
          value={active === undefined ? "all" : String(active)}
          onChange={(event) => onActiveChange(event.target.value === "all" ? undefined : event.target.value === "true")}
          className="h-11 w-full rounded-md border border-input bg-white px-3 text-sm"
        >
          <option value="true">Activos</option>
          <option value="false">Inactivos</option>
          <option value="all">Todos</option>
        </select>
      </label>
      <label className="space-y-2 text-sm font-medium">
        Deporte
        <Input value={sport} onChange={(event) => onSportChange(event.target.value)} placeholder="Ej: Futbol" />
      </label>
      <label className="space-y-2 text-sm font-medium">
        Nivel
        <Input value={level} onChange={(event) => onLevelChange(event.target.value)} placeholder="Ej: Intermedio" />
      </label>
      <Button type="button" variant="outline" className="w-full" onClick={onClear}>
        Limpiar
      </Button>
    </div>
  )
}
