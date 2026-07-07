import { ExerciseCard } from "@/components/exercise/ExerciseCard"
import type { ExerciseSummary } from "@/types/exercise"

interface ExerciseListProps {
  exercises: ExerciseSummary[]
  onToggleActive: (exercise: ExerciseSummary) => void
  pendingExerciseId?: number | null
}

export function ExerciseList({ exercises, onToggleActive, pendingExerciseId }: ExerciseListProps) {
  return (
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
      {exercises.map((exercise) => (
        <ExerciseCard key={exercise.id} exercise={exercise} onToggleActive={onToggleActive} isTogglePending={pendingExerciseId === exercise.id} />
      ))}
    </div>
  )
}
