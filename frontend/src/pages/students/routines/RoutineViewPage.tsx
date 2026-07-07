import { useParams } from "react-router-dom"
import { RoutineActionsBar } from "@/components/routine/RoutineActionsBar"
import { RoutineIdentityHeader } from "@/components/routine/RoutineIdentityHeader"
import { BackButton } from "@/components/shared/BackButton"
import { TrainingPlanReadOnlyView } from "@/components/template/TrainingPlanReadOnlyView"
import { Skeleton } from "@/components/ui/skeleton"
import { useRoutine } from "@/hooks/useRoutines"
import { useStudent } from "@/hooks/useStudents"

export function RoutineViewPage() {
  const routeStudentId = Number(useParams().studentId)
  const routineId = Number(useParams().routineId)
  const routineQuery = useRoutine(routineId)
  const routine = routineQuery.data
  const studentId = routine?.studentId ?? routeStudentId
  const studentQuery = useStudent(studentId)

  if (routineQuery.isLoading) return <RoutineViewSkeleton />
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

function RoutineViewSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-9 w-24" />
      <div className="mb-6 flex flex-col gap-4 rounded-md border bg-white p-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="space-y-3">
          <div className="flex flex-wrap items-center gap-2">
            <Skeleton className="h-7 w-56" />
            <Skeleton className="h-6 w-20 rounded-full" />
          </div>
          <div className="flex flex-wrap gap-3">
            <Skeleton className="h-4 w-28" />
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-4 w-32" />
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <Skeleton className="h-8 w-28" />
          <Skeleton className="h-8 w-28" />
          <Skeleton className="h-8 w-24" />
        </div>
      </div>
      <div className="space-y-4 rounded-md border bg-white p-4">
        <Skeleton className="h-6 w-32" />
        <div className="grid gap-3">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      </div>
    </div>
  )
}
