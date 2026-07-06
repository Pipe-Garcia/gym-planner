import { Edit, Eye, Loader2, RotateCcw, UserMinus } from "lucide-react"
import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"
import type { StudentSummary } from "@/types/student"

interface StudentListProps {
  students: StudentSummary[]
  onToggleActive: (student: StudentSummary) => void
  pendingStudentId?: number | null
}

export function StudentList({ students, onToggleActive, pendingStudentId }: StudentListProps) {
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
            {students.map((student) => {
              const isPending = pendingStudentId === student.id
              return (
              <tr key={student.id} className="border-t">
                <td className="px-4 py-3 font-medium">
                  {student.lastName}, {student.firstName}
                </td>
                <td className="px-4 py-3">{student.documentId || "-"}</td>
                <td className="px-4 py-3">{student.phone || "-"}</td>
                <td className="px-4 py-3">{student.sport || "-"}</td>
                <td className="px-4 py-3">{student.level || "-"}</td>
                <td className="px-4 py-3">
                  <Status active={student.active} />
                </td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-2">
                    <Button type="button" variant="ghost" size="icon" asChild aria-label="Ver alumno">
                      <Link to={`/students/${student.id}`}>
                        <Eye className="h-4 w-4" />
                      </Link>
                    </Button>
                    <Button type="button" variant="ghost" size="icon" asChild aria-label="Editar alumno">
                      <Link to={`/students/${student.id}/edit`}>
                        <Edit className="h-4 w-4" />
                      </Link>
                    </Button>
                    <Button type="button" variant="ghost" size="icon" onClick={() => onToggleActive(student)} disabled={isPending} aria-label="Cambiar estado">
                      {isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : student.active ? <UserMinus className="h-4 w-4" /> : <RotateCcw className="h-4 w-4" />}
                    </Button>
                  </div>
                </td>
              </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      <div className="grid gap-3 md:hidden">
        {students.map((student) => {
          const isPending = pendingStudentId === student.id
          return (
          <article key={student.id} className="rounded-md border bg-white p-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h3 className="font-semibold">
                  {student.lastName}, {student.firstName}
                </h3>
                <p className="mt-1 text-sm text-muted-foreground">{student.documentId || "Sin DNI"}</p>
              </div>
              <Status active={student.active} />
            </div>
            <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
              <div>
                <dt className="text-muted-foreground">Teléfono</dt>
                <dd>{student.phone || "-"}</dd>
              </div>
              <div>
                <dt className="text-muted-foreground">Nivel</dt>
                <dd>{student.level || "-"}</dd>
              </div>
              <div className="col-span-2">
                <dt className="text-muted-foreground">Deporte</dt>
                <dd>{student.sport || "-"}</dd>
              </div>
            </dl>
            <div className="mt-4 flex gap-2">
              <Button type="button" variant="outline" className="flex-1" asChild>
                <Link to={`/students/${student.id}`}>Ver</Link>
              </Button>
              <Button type="button" variant="outline" className="flex-1" asChild>
                <Link to={`/students/${student.id}/edit`}>Editar</Link>
              </Button>
              <Button type="button" variant="ghost" size="icon" onClick={() => onToggleActive(student)} disabled={isPending} aria-label="Cambiar estado">
                {isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : student.active ? <UserMinus className="h-4 w-4" /> : <RotateCcw className="h-4 w-4" />}
              </Button>
            </div>
          </article>
          )
        })}
      </div>
    </div>
  )
}

function Status({ active }: { active: boolean }) {
  return (
    <span className={active ? "rounded-full bg-emerald-100 px-2 py-1 text-xs font-medium text-emerald-700" : "rounded-full bg-slate-100 px-2 py-1 text-xs font-medium text-slate-600"}>
      {active ? "Activo" : "Inactivo"}
    </span>
  )
}
