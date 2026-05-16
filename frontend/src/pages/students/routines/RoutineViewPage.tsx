import { useParams } from "react-router-dom"
import { RoutineActionsBar } from "@/components/routine/RoutineActionsBar"
import { RoutineIdentityHeader } from "@/components/routine/RoutineIdentityHeader"
import { BackButton } from "@/components/shared/BackButton"
import { TrainingPlanReadOnlyView } from "@/components/template/TrainingPlanReadOnlyView"
import { useRoutine } from "@/hooks/useRoutines"

export function RoutineViewPage() {
  const studentId = Number(useParams().studentId)
  const routineId = Number(useParams().routineId)
  const routineQuery = useRoutine(routineId)
  const routine = routineQuery.data

  if (routineQuery.isLoading) return <div className="rounded-md border bg-white p-6 text-sm text-muted-foreground">Cargando rutina...</div>
  if (!routine) return <div className="rounded-md border bg-white p-6 text-sm text-muted-foreground">Rutina no encontrada.</div>

  return (
    <div className="space-y-6">
      <BackButton to={`/students/${routine.studentId}`} />
      <div className="mb-6 flex flex-col gap-4 rounded-md border bg-white p-4 lg:flex-row lg:items-start lg:justify-between">
        <RoutineIdentityHeader routine={routine} />
        <RoutineActionsBar routine={routine} studentId={studentId} mode="view" onRoutineChanged={() => { void routineQuery.refetch() }} />
      </div>
      <TrainingPlanReadOnlyView days={routine.days} context="routine" />
    </div>
  )
}
