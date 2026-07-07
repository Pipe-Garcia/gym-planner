import { Filter, Plus, Search } from "lucide-react"
import { useState } from "react"
import { Link } from "react-router-dom"
import { EmptyState } from "@/components/shared/EmptyState"
import { ConfirmDialog } from "@/components/shared/ConfirmDialog"
import { LoadingSpinner } from "@/components/shared/LoadingSpinner"
import { ExerciseList } from "@/components/exercise/ExerciseList"
import { TagFilter } from "@/components/exercise/TagFilter"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { useDeactivateExercise, useExercises, useExerciseTags, useReactivateExercise } from "@/hooks/useExercises"
import { useToast } from "@/hooks/useToast"
import type { ExerciseSummary } from "@/types/exercise"

export function ExercisesListPage() {
  const toast = useToast()
  const [search, setSearch] = useState("")
  const [tagIds, setTagIds] = useState<number[]>([])
  const [showInactive, setShowInactive] = useState(false)
  const [page, setPage] = useState(0)
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [pendingExerciseId, setPendingExerciseId] = useState<number | null>(null)
  const [exerciseToDeactivate, setExerciseToDeactivate] = useState<ExerciseSummary | null>(null)
  const tagsQuery = useExerciseTags()
  const exercisesQuery = useExercises({ search, tagIds, active: showInactive ? undefined : true, page, size: 20, sort: "name,asc" })
  const deactivate = useDeactivateExercise()
  const reactivate = useReactivateExercise()

  async function toggleActive(exercise: ExerciseSummary) {
    if (exercise.active) {
      setExerciseToDeactivate(exercise)
      return
    }
    setPendingExerciseId(exercise.id)
    try {
      await reactivate.mutateAsync(exercise.id)
      toast.success("Ejercicio reactivado.")
    } catch {
      toast.error("No pudimos cambiar el estado.")
    } finally {
      setPendingExerciseId(null)
    }
  }

  async function confirmDeactivateExercise() {
    if (!exerciseToDeactivate) return
    setPendingExerciseId(exerciseToDeactivate.id)
    try {
      await deactivate.mutateAsync(exerciseToDeactivate.id)
      toast.success("Ejercicio desactivado.")
      setExerciseToDeactivate(null)
    } catch {
      toast.error("No pudimos cambiar el estado.")
    } finally {
      setPendingExerciseId(null)
    }
  }

  const filters = <TagFilter tags={tagsQuery.data ?? []} selectedIds={tagIds} onChange={setTagIds} />

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-normal">Ejercicios</h1>
          <p className="mt-1 text-sm text-muted-foreground">Catalogo del gimnasio con etiquetas y filtros.</p>
        </div>
        <Button asChild>
          <Link to="/exercises/new">
            <Plus className="h-4 w-4" />
            Nuevo ejercicio
          </Link>
        </Button>
      </div>

      <div className="grid gap-4 lg:grid-cols-[280px_1fr]">
        <aside className="hidden lg:block">{filters}</aside>
        <section className="space-y-4">
          <div className="flex flex-col gap-3 sm:flex-row">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
              <Input
                value={search}
                onChange={(event) => {
                  setSearch(event.target.value)
                  setPage(0)
                }}
                placeholder="Buscar ejercicio"
                className="pl-10"
              />
            </div>
            <label className="flex min-h-11 items-center gap-2 rounded-md border bg-white px-3 text-sm">
              <input type="checkbox" checked={showInactive} onChange={(event) => setShowInactive(event.target.checked)} />
              Mostrar inactivos
            </label>
            <Button type="button" variant="outline" size="icon" className="lg:hidden" onClick={() => setFiltersOpen(true)} aria-label="Filtros">
              <Filter className="h-4 w-4" />
            </Button>
          </div>

          {exercisesQuery.isLoading ? (
            <div className="flex min-h-64 items-center justify-center">
              <LoadingSpinner />
            </div>
          ) : exercisesQuery.data?.content.length ? (
            <>
              <ExerciseList exercises={exercisesQuery.data.content} onToggleActive={toggleActive} pendingExerciseId={pendingExerciseId} />
              <Pagination page={exercisesQuery.data.page} totalPages={exercisesQuery.data.totalPages} onPage={setPage} />
            </>
          ) : (
            <EmptyState title="No hay ejercicios" description="Ajusta la busqueda o crea el primer ejercicio." />
          )}
        </section>
      </div>

      <Dialog open={filtersOpen} onOpenChange={setFiltersOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Filtros</DialogTitle>
          </DialogHeader>
          {filters}
        </DialogContent>
      </Dialog>
      <ConfirmDialog
        open={Boolean(exerciseToDeactivate)}
        onOpenChange={(open) => {
          if (!open) setExerciseToDeactivate(null)
        }}
        title="Desactivar ejercicio"
        description="El ejercicio dejará de estar disponible para nuevas rutinas, pero no se elimina el historial existente."
        confirmLabel="Desactivar"
        loadingLabel="Desactivando..."
        isPending={deactivate.isPending}
        onConfirm={() => {
          void confirmDeactivateExercise()
        }}
      />
    </div>
  )
}

function Pagination({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (page: number) => void }) {
  return (
    <div className="flex items-center justify-between rounded-md border bg-white p-3 text-sm">
      <span>
        Pagina {page + 1} de {Math.max(totalPages, 1)}
      </span>
      <div className="flex gap-2">
        <Button type="button" variant="outline" disabled={page === 0} onClick={() => onPage(page - 1)}>
          Anterior
        </Button>
        <Button type="button" variant="outline" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>
          Siguiente
        </Button>
      </div>
    </div>
  )
}
