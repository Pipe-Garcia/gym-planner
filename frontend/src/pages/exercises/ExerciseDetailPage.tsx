import { Edit, Loader2, RotateCcw, XCircle } from "lucide-react"
import { Link, useParams } from "react-router-dom"
import { LoadingSpinner } from "@/components/shared/LoadingSpinner"
import { BackButton } from "@/components/shared/BackButton"
import { TagBadge } from "@/components/exercise/TagBadge"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { useDeactivateExercise, useExercise, useReactivateExercise } from "@/hooks/useExercises"
import { useToast } from "@/hooks/useToast"
import { measurementTypeLabel } from "@/lib/format"

export function ExerciseDetailPage() {
  const toast = useToast()
  const id = Number(useParams().id)
  const exerciseQuery = useExercise(id)
  const deactivate = useDeactivateExercise()
  const reactivate = useReactivateExercise()
  const isTogglePending = deactivate.isPending || reactivate.isPending

  if (exerciseQuery.isLoading) {
    return (
      <div className="flex min-h-80 items-center justify-center">
        <LoadingSpinner />
      </div>
    )
  }

  const exercise = exerciseQuery.data
  if (!exercise) {
    return <p className="text-sm text-muted-foreground">Ejercicio no encontrado.</p>
  }

  async function toggleActive() {
    if (!exercise) return
    try {
      if (exercise.active) {
        await deactivate.mutateAsync(exercise.id)
        toast.success("Ejercicio desactivado.")
      } else {
        await reactivate.mutateAsync(exercise.id)
        toast.success("Ejercicio reactivado.")
      }
      await exerciseQuery.refetch()
    } catch {
      toast.error("No pudimos cambiar el estado.")
    }
  }

  return (
    <div className="space-y-6">
      <BackButton to="/exercises" />
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-2xl font-semibold tracking-normal">{exercise.name}</h1>
            <span className={exercise.active ? "rounded-full bg-emerald-100 px-2 py-1 text-xs font-medium text-emerald-700" : "rounded-full bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600"}>
              {exercise.active ? "Activo" : "Inactivo"}
            </span>
          </div>
          <p className="mt-1 text-sm text-muted-foreground">{measurementTypeLabel(exercise.defaultMeasurement)}</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" asChild>
            <Link to={`/exercises/${exercise.id}/edit`}>
              <Edit className="h-4 w-4" />
              Editar
            </Link>
          </Button>
          <Button type="button" variant={exercise.active ? "destructive" : "outline"} onClick={toggleActive} disabled={isTogglePending}>
            {isTogglePending ? <Loader2 className="h-4 w-4 animate-spin" /> : exercise.active ? <XCircle className="h-4 w-4" /> : <RotateCcw className="h-4 w-4" />}
            {isTogglePending ? (exercise.active ? "Desactivando..." : "Reactivando...") : exercise.active ? "Desactivar" : "Reactivar"}
          </Button>
        </div>
      </div>

      <Card>
        <CardContent className="space-y-5 p-6">
          <div className="flex flex-wrap gap-2">
            {exercise.tags.map((tag) => (
              <TagBadge key={tag.id} tag={tag} />
            ))}
          </div>
          <div>
            <h2 className="font-semibold">Descripción</h2>
            <p className="mt-2 whitespace-pre-wrap text-sm text-muted-foreground">{exercise.description || "-"}</p>
          </div>
          <div>
            <h2 className="font-semibold">Notas técnicas</h2>
            <p className="mt-2 whitespace-pre-wrap text-sm text-muted-foreground">{exercise.technicalNotes || "-"}</p>
          </div>
          {exercise.imageUrl || exercise.videoUrl ? (
            <div className="grid gap-3 text-sm">
              {exercise.imageUrl ? <a className="text-primary underline" href={exercise.imageUrl} target="_blank" rel="noreferrer">Imagen</a> : null}
              {exercise.videoUrl ? <a className="text-primary underline" href={exercise.videoUrl} target="_blank" rel="noreferrer">Video</a> : null}
            </div>
          ) : null}
        </CardContent>
      </Card>
    </div>
  )
}
