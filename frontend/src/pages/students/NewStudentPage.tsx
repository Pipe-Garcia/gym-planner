import { BackButton } from "@/components/shared/BackButton"
import { StudentForm } from "@/components/student/StudentForm"
import { useCreateStudent } from "@/hooks/useStudents"
import type { StudentFormValues } from "@/schemas/student.schema"

export function NewStudentPage() {
  const createStudent = useCreateStudent()

  return (
    <div className="space-y-6">
      <BackButton to="/students" />
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">Nuevo alumno</h1>
        <p className="mt-1 text-sm text-muted-foreground">Carga los datos base del alumno.</p>
      </div>
      <StudentForm onSubmit={(values: StudentFormValues) => createStudent.mutateAsync(values)} submitLabel="Crear alumno" />
    </div>
  )
}
