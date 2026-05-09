import { useParams } from "react-router-dom"
import { LoadingSpinner } from "@/components/shared/LoadingSpinner"
import { BackButton } from "@/components/shared/BackButton"
import { ExerciseForm } from "@/components/exercise/ExerciseForm"
import { useExercise, useExerciseTags, useUpdateExercise } from "@/hooks/useExercises"
import type { ExerciseFormValues } from "@/schemas/exercise.schema"

export function EditExercisePage() {
  const id = Number(useParams().id)
  const exerciseQuery = useExercise(id)
  const tagsQuery = useExerciseTags()
  const updateExercise = useUpdateExercise(id)

  if (exerciseQuery.isLoading || tagsQuery.isLoading) {
    return (
      <div className="flex min-h-80 items-center justify-center">
        <LoadingSpinner />
      </div>
    )
  }

  if (!exerciseQuery.data) {
    return <p className="text-sm text-muted-foreground">Ejercicio no encontrado.</p>
  }

  return (
    <div className="space-y-6">
      <BackButton to={`/exercises/${id}`} />
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">Editar ejercicio</h1>
        <p className="mt-1 text-sm text-muted-foreground">{exerciseQuery.data.name}</p>
      </div>
      <ExerciseForm
        tags={tagsQuery.data ?? []}
        initialData={exerciseQuery.data}
        onSubmit={(values: ExerciseFormValues) => updateExercise.mutateAsync(values)}
        submitLabel="Guardar cambios"
      />
    </div>
  )
}
