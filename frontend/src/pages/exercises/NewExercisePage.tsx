import { LoadingSpinner } from "@/components/shared/LoadingSpinner"
import { BackButton } from "@/components/shared/BackButton"
import { ExerciseForm } from "@/components/exercise/ExerciseForm"
import { useCreateExercise, useExerciseTags } from "@/hooks/useExercises"
import type { ExerciseFormValues } from "@/schemas/exercise.schema"

export function NewExercisePage() {
  const tagsQuery = useExerciseTags()
  const createExercise = useCreateExercise()

  if (tagsQuery.isLoading) {
    return (
      <div className="flex min-h-80 items-center justify-center">
        <LoadingSpinner />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <BackButton to="/exercises" />
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">Nuevo ejercicio</h1>
        <p className="mt-1 text-sm text-muted-foreground">Carga el ejercicio y sus tags.</p>
      </div>
      <ExerciseForm tags={tagsQuery.data ?? []} onSubmit={(values: ExerciseFormValues) => createExercise.mutateAsync(values)} submitLabel="Crear ejercicio" />
    </div>
  )
}
