import { useParams } from "react-router-dom"
import { RoutineActionsBar } from "@/components/routine/RoutineActionsBar"
import { RoutineIdentityHeader } from "@/components/routine/RoutineIdentityHeader"
import { BackButton } from "@/components/shared/BackButton"
import { TrainingPlanReadOnlyView } from "@/components/template/TrainingPlanReadOnlyView"
import { useRoutine } from "@/hooks/useRoutines"
import { useStudent } from "@/hooks/useStudents"

export function RoutineViewPage() {
  const routeStudentId = Number(useParams().studentId)
  const routineId = Number(useParams().routineId)
  const routineQuery = useRoutine(routineId)
  const routine = routineQuery.data
  const studentId = routine?.studentId ?? routeStudentId
  const studentQuery = useStudent(studentId)

  if (routineQuery.isLoading) return <div className="rounded-md border bg-white p-6 text-sm text-muted-foreground">Cargando rutina...</div>
  if (!routine) return <div className="rounded-md border bg-white p-6 text-sm text-muted-foreground">Rutina no encontrada.</div>

  return (
    <div className="space-y-6">
      <BackButton to={`/students/${routine.studentId}`} />
      <div className="mb-6 flex flex-col gap-4 rounded-md border bg-white p-4 lg:flex-row lg:items-start lg:justify-between">
        <RoutineIdentityHeader routine={routine} />
        <RoutineActionsBar
          routine={routine}
          studentId={routine.studentId}
          mode="view"
          studentFirstName={studentQuery.data?.firstName}
          studentPhone={studentQuery.data?.phone}
          studentLoading={studentQuery.isLoading}
          studentError={studentQuery.isError}
          onRoutineChanged={() => { void routineQuery.refetch() }}
        />
      </div>
      <TrainingPlanReadOnlyView days={routine.days} context="routine" />
    </div>
  )
}
