import { Link } from "react-router-dom"
import { formatDateEs } from "@/lib/date"
import { routineStatusBadgeClass, routineStatusLabel } from "@/lib/labels"
import type { RoutineResponse, RoutineSummaryResponse } from "@/types/training"

interface RoutineIdentityHeaderProps {
  routine: RoutineResponse | RoutineSummaryResponse
}

export function RoutineIdentityHeader({ routine }: RoutineIdentityHeaderProps) {
  return (
    <div className="min-w-0 space-y-2">
      <div className="flex flex-wrap items-center gap-2">
        <h1 className="break-words text-2xl font-semibold leading-tight tracking-normal">{routine.name}</h1>
        <span className={routineStatusBadgeClass(routine.status)}>{routineStatusLabel(routine.status)}</span>
      </div>
      <div className="flex flex-wrap gap-x-2 gap-y-1 text-sm text-muted-foreground">
        <Link className="font-medium text-foreground hover:underline" to={`/students/${routine.studentId}`}>{routine.studentName}</Link>
        <span>·</span>
        <span>{formatDateEs(routine.assignedDate)}</span>
        {routine.objective ? <><span>·</span><span>{routine.objective}</span></> : null}
      </div>
      <p className="text-sm text-muted-foreground">
        {routine.previousRoutineId ? (
          <span>Continuación de <Link className="text-primary hover:underline" to={`/students/${routine.studentId}/routines/${routine.previousRoutineId}`}>rutina #{routine.previousRoutineId}</Link></span>
        ) : routine.sourceTemplateName || routine.sourceTemplateId ? (
          <span>Origen: {routine.sourceTemplateName || `Plantilla #${routine.sourceTemplateId}`}</span>
        ) : (
          <span>Sin plantilla origen</span>
        )}
      </p>
    </div>
  )
}
