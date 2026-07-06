import { Filter, Plus } from "lucide-react"
import { useState } from "react"
import { Link } from "react-router-dom"
import { EmptyState } from "@/components/shared/EmptyState"
import { LoadingSpinner } from "@/components/shared/LoadingSpinner"
import { StudentFilters } from "@/components/student/StudentFilters"
import { StudentList } from "@/components/student/StudentList"
import { StudentSearchBar } from "@/components/student/StudentSearchBar"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { useDeactivateStudent, useReactivateStudent, useStudents } from "@/hooks/useStudents"
import { useToast } from "@/hooks/useToast"
import type { StudentSummary } from "@/types/student"

export function StudentsListPage() {
  const toast = useToast()
  const [search, setSearch] = useState("")
  const [active, setActive] = useState<boolean | undefined>(true)
  const [sport, setSport] = useState("")
  const [level, setLevel] = useState("")
  const [page, setPage] = useState(0)
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [pendingStudentId, setPendingStudentId] = useState<number | null>(null)
  const studentsQuery = useStudents({ search, active, sport, level, page, size: 20, sort: "lastName,asc" })
  const totalStudents = studentsQuery.data?.totalElements
  const deactivate = useDeactivateStudent()
  const reactivate = useReactivateStudent()

  async function toggleActive(student: StudentSummary) {
    setPendingStudentId(student.id)
    try {
      if (student.active) {
        await deactivate.mutateAsync(student.id)
        toast.success("Alumno desactivado.")
      } else {
        await reactivate.mutateAsync(student.id)
        toast.success("Alumno reactivado.")
      }
    } catch {
      toast.error("No pudimos cambiar el estado.")
    } finally {
      setPendingStudentId(null)
    }
  }

  const filters = (
    <StudentFilters
      active={active}
      sport={sport}
      level={level}
      onActiveChange={(value) => {
        setActive(value)
        setPage(0)
      }}
      onSportChange={(value) => {
        setSport(value)
        setPage(0)
      }}
      onLevelChange={(value) => {
        setLevel(value)
        setPage(0)
      }}
      onClear={() => {
        setActive(true)
        setSport("")
        setLevel("")
        setPage(0)
      }}
    />
  )

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-normal">Alumnos</h1>
          <p className="mt-1 text-sm text-muted-foreground">Gestión de alumnos, lesiones y notas internas.</p>
        </div>
        <Button asChild>
          <Link to="/students/new">
            <Plus className="h-4 w-4" />
            Nuevo alumno
          </Link>
        </Button>
      </div>

      <div className="grid gap-4 lg:grid-cols-[260px_1fr]">
        <aside className="hidden lg:block">{filters}</aside>
        <section className="space-y-4">
          <div className="flex gap-2">
            <div className="flex-1">
              <StudentSearchBar
                value={search}
                onChange={(value) => {
                  setSearch(value)
                  setPage(0)
                }}
              />
            </div>
            <Button type="button" variant="outline" size="icon" className="lg:hidden" onClick={() => setFiltersOpen(true)} aria-label="Filtros">
              <Filter className="h-4 w-4" />
            </Button>
          </div>

          {totalStudents !== undefined && (
            <p className="text-sm text-muted-foreground">
              Mostrando {totalStudents} {totalStudents === 1 ? "alumno" : "alumnos"}
            </p>
          )}

          {studentsQuery.isLoading ? (
            <div className="flex min-h-64 items-center justify-center">
              <LoadingSpinner />
            </div>
          ) : studentsQuery.data?.content.length ? (
            <>
              <StudentList students={studentsQuery.data.content} onToggleActive={toggleActive} pendingStudentId={pendingStudentId} />
              <Pagination page={studentsQuery.data.page} totalPages={studentsQuery.data.totalPages} onPage={setPage} />
            </>
          ) : (
            <EmptyState title="No hay alumnos" description="Ajusta la busqueda o crea el primer alumno." />
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
