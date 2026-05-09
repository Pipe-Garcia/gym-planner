import { useParams } from "react-router-dom"
import { LoadingSpinner } from "@/components/shared/LoadingSpinner"
import { BackButton } from "@/components/shared/BackButton"
import { StudentForm } from "@/components/student/StudentForm"
import { useStudent, useUpdateStudent } from "@/hooks/useStudents"
import type { StudentFormValues } from "@/schemas/student.schema"

export function EditStudentPage() {
  const id = Number(useParams().id)
  const studentQuery = useStudent(id)
  const updateStudent = useUpdateStudent(id)

  if (studentQuery.isLoading) {
    return (
      <div className="flex min-h-80 items-center justify-center">
        <LoadingSpinner />
      </div>
    )
  }

  if (!studentQuery.data) {
    return <p className="text-sm text-muted-foreground">Alumno no encontrado.</p>
  }

  return (
    <div className="space-y-6">
      <BackButton to={`/students/${id}`} />
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">Editar alumno</h1>
        <p className="mt-1 text-sm text-muted-foreground">{studentQuery.data.firstName} {studentQuery.data.lastName}</p>
      </div>
      <StudentForm initialData={studentQuery.data} onSubmit={(values: StudentFormValues) => updateStudent.mutateAsync(values)} submitLabel="Guardar cambios" />
    </div>
  )
}
