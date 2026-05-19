import { History } from "lucide-react"
import { EmptyState } from "@/components/shared/EmptyState"

export function StudentHistoryEmptyState() {
  return (
    <EmptyState
      icon={<History className="h-8 w-8" />}
      title="Este alumno aún no tiene rutinas asignadas."
      description="Cuando crees una rutina, su historial aparecerá acá."
      className="bg-white"
    />
  )
}
