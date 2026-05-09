import { Edit, Eye, RotateCcw, XCircle } from "lucide-react"
import { Link } from "react-router-dom"
import { TagBadge } from "@/components/exercise/TagBadge"
import { Button } from "@/components/ui/button"
import { measurementTypeLabel } from "@/lib/format"
import type { ExerciseSummary } from "@/types/exercise"

interface ExerciseCardProps {
  exercise: ExerciseSummary
  onToggleActive: (exercise: ExerciseSummary) => void
}

export function ExerciseCard({ exercise, onToggleActive }: ExerciseCardProps) {
  return (
    <article className="rounded-md border bg-white p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="font-semibold">{exercise.name}</h3>
          <p className="mt-1 text-sm text-muted-foreground">{measurementTypeLabel(exercise.defaultMeasurement)}</p>
        </div>
        <span className={exercise.active ? "rounded-full bg-emerald-100 px-2 py-1 text-xs font-medium text-emerald-700" : "rounded-full bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600"}>
          {exercise.active ? "Activo" : "Inactivo"}
        </span>
      </div>
      <div className="mt-3 flex flex-wrap gap-2">
        {exercise.tags.slice(0, 6).map((tag) => (
          <TagBadge key={tag.id} tag={tag} />
        ))}
      </div>
      <div className="mt-4 flex gap-2">
        <Button type="button" variant="outline" className="flex-1" asChild>
          <Link to={`/exercises/${exercise.id}`}>
            <Eye className="h-4 w-4" />
            Ver
          </Link>
        </Button>
        <Button type="button" variant="outline" className="flex-1" asChild>
          <Link to={`/exercises/${exercise.id}/edit`}>
            <Edit className="h-4 w-4" />
            Editar
          </Link>
        </Button>
        <Button type="button" variant="ghost" size="icon" onClick={() => onToggleActive(exercise)} aria-label="Cambiar estado">
          {exercise.active ? <XCircle className="h-4 w-4" /> : <RotateCcw className="h-4 w-4" />}
        </Button>
      </div>
    </article>
  )
}
