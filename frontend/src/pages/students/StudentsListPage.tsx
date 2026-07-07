import { Filter, Plus } from "lucide-react"
import { useState } from "react"
import { Link } from "react-router-dom"
import { EmptyState } from "@/components/shared/EmptyState"
import { ConfirmDialog } from "@/components/shared/ConfirmDialog"
import { StudentFilters } from "@/components/student/StudentFilters"
import { StudentList } from "@/components/student/StudentList"
import { StudentSearchBar } from "@/components/student/StudentSearchBar"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Skeleton } from "@/components/ui/skeleton"
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
  const [studentToDeactivate, setStudentToDeactivate] = useState<StudentSummary | null>(null)
  const studentsQuery = useStudents({ search, active, sport, level, page, size: 20, sort: "lastName,asc" })
  const totalStudents = studentsQuery.data?.totalElements
  const deactivate = useDeactivateStudent()
  const reactivate = useReactivateStudent()

  async function toggleActive(student: StudentSummary) {
    if (student.active) {
      setStudentToDeactivate(student)
      return
    }
    setPendingStudentId(student.id)
    try {
      await reactivate.mutateAsync(student.id)
      toast.success("Alumno reactivado.")
    } catch {
      toast.error("No pudimos cambiar el estado.")
    } finally {
      setPendingStudentId(null)
    }
  }

  async function confirmDeactivateStudent() {
    if (!studentToDeactivate) return
    setPendingStudentId(studentToDeactivate.id)
    try {
      await deactivate.mutateAsync(studentToDeactivate.id)
      toast.success("Alumno desactivado.")
      setStudentToDeactivate(null)
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
            <StudentsListSkeleton />
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
      <ConfirmDialog
        open={Boolean(studentToDeactivate)}
        onOpenChange={(open) => {
          if (!open) setStudentToDeactivate(null)
        }}
        title="Desactivar alumno"
        description="El alumno dejará de aparecer en los listados activos. Podés reactivarlo más adelante."
        confirmLabel="Desactivar"
        loadingLabel="Desactivando..."
        isPending={deactivate.isPending}
        onConfirm={() => {
          void confirmDeactivateStudent()
        }}
      />
    </div>
  )
}

function StudentsListSkeleton() {
  return (
    <div>
      <div className="hidden overflow-x-auto rounded-md border bg-white md:block">
        <table className="w-full text-sm">
          <thead className="bg-muted/70 text-left">
            <tr>
              <th className="px-4 py-3 font-medium">Apellido y nombre</th>
              <th className="px-4 py-3 font-medium">DNI</th>
              <th className="px-4 py-3 font-medium">Teléfono</th>
              <th className="px-4 py-3 font-medium">Deporte</th>
              <th className="px-4 py-3 font-medium">Nivel</th>
              <th className="px-4 py-3 font-medium">Estado</th>
              <th className="px-4 py-3 text-right font-medium">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: 6 }).map((_, index) => (
              <tr key={index} className="border-t">
                <td className="px-4 py-3"><Skeleton className="h-4 w-40" /></td>
                <td className="px-4 py-3"><Skeleton className="h-4 w-24" /></td>
                <td className="px-4 py-3"><Skeleton className="h-4 w-28" /></td>
                <td className="px-4 py-3"><Skeleton className="h-4 w-24" /></td>
                <td className="px-4 py-3"><Skeleton className="h-4 w-20" /></td>
                <td className="px-4 py-3"><Skeleton className="h-6 w-16 rounded-full" /></td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-2">
                    <Skeleton className="h-9 w-9" />
                    <Skeleton className="h-9 w-9" />
                    <Skeleton className="h-9 w-9" />
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="grid gap-3 md:hidden">
        {Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="rounded-md border bg-white p-4">
            <div className="flex items-start justify-between gap-3">
              <div className="space-y-2">
                <Skeleton className="h-5 w-36" />
                <Skeleton className="h-4 w-24" />
              </div>
              <Skeleton className="h-6 w-16 rounded-full" />
            </div>
            <div className="mt-4 grid grid-cols-2 gap-3">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-4 w-20" />
              <Skeleton className="h-4 w-28" />
            </div>
            <div className="mt-4 flex gap-2">
              <Skeleton className="h-9 flex-1" />
              <Skeleton className="h-9 flex-1" />
              <Skeleton className="h-9 w-9" />
            </div>
          </div>
        ))}
      </div>
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
