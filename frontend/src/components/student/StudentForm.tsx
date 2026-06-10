import { zodResolver } from "@hookform/resolvers/zod"
import type { AxiosError } from "axios"
import { Save } from "lucide-react"
import { useEffect } from "react"
import { useForm } from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { studentFormSchema, type StudentFormValues } from "@/schemas/student.schema"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useToast } from "@/hooks/useToast"
import type { ApiError } from "@/types/api"
import type { Student } from "@/types/student"

interface StudentFormProps {
  initialData?: Student
  onSubmit: (values: StudentFormValues) => Promise<Student>
  submitLabel: string
}

const defaults: StudentFormValues = {
  firstName: "",
  lastName: "",
  documentId: "",
  phone: "",
  email: "",
  birthDate: "",
  sport: "",
  objective: "",
  level: "",
  generalNotes: "",
  startedAt: "",
}

export function StudentForm({ initialData, onSubmit, submitLabel }: StudentFormProps) {
  const toast = useToast()
  const navigate = useNavigate()
  const form = useForm<StudentFormValues>({
    resolver: zodResolver(studentFormSchema),
    defaultValues: defaults,
  })

  useEffect(() => {
    if (initialData) {
      form.reset({
        firstName: initialData.firstName,
        lastName: initialData.lastName,
        documentId: initialData.documentId ?? "",
        phone: initialData.phone ?? "",
        email: initialData.email ?? "",
        birthDate: initialData.birthDate ?? "",
        sport: initialData.sport ?? "",
        objective: initialData.objective ?? "",
        level: initialData.level ?? "",
        generalNotes: initialData.generalNotes ?? "",
        startedAt: initialData.startedAt ?? "",
      })
    }
  }, [form, initialData])

  async function handleSubmit(values: StudentFormValues) {
    try {
      const saved = await onSubmit(values)
      toast.success("Alumno guardado.")
      navigate(`/students/${saved.id}`)
    } catch (error) {
      const apiError = error as AxiosError<ApiError>
      if (apiError.response?.status === 409) {
        const fieldErrors = apiError.response.data.fieldErrors ?? {}
        let hasMappedFieldError = false

        Object.entries(fieldErrors).forEach(([fieldName, message]) => {
          if (isStudentFormField(fieldName)) {
            form.setError(fieldName, { type: "server", message })
            hasMappedFieldError = true
          }
        })

        if (hasMappedFieldError) return
      }
      toast.error(apiError.response?.data.message ?? "No pudimos guardar el alumno.")
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-5">
        <Section title="Datos personales">
          {renderTextField("firstName", "Nombre")}
          {renderTextField("lastName", "Apellido")}
          {renderTextField("documentId", "DNI / identificador")}
          {renderTextField("birthDate", "Fecha de nacimiento", "date")}
        </Section>
        <Section title="Contacto">
          {renderTextField("phone", "Teléfono")}
          {renderTextField("email", "Email", "email")}
        </Section>
        <Section title="Entrenamiento">
          {renderTextField("sport", "Deporte")}
          {renderTextField("objective", "Objetivo")}
          {renderTextField("level", "Nivel")}
          {renderTextField("startedAt", "Inicio en el gym", "date")}
        </Section>
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Notas generales</CardTitle>
          </CardHeader>
          <CardContent>
            <FormField
              control={form.control}
              name="generalNotes"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Notas generales</FormLabel>
                  <FormControl>
                    <textarea className="min-h-28 w-full rounded-md border border-input bg-white px-3 py-2 text-sm" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>
        <Button type="submit" disabled={form.formState.isSubmitting}>
          <Save className="h-4 w-4" />
          {form.formState.isSubmitting ? "Guardando..." : submitLabel}
        </Button>
      </form>
    </Form>
  )

  function renderTextField(name: keyof StudentFormValues, label: string, type = "text") {
    return (
      <FormField
        key={name}
        control={form.control}
        name={name}
        render={({ field }) => (
          <FormItem>
            <FormLabel>{label}</FormLabel>
            <FormControl>
              <Input type={type} {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    )
  }
}

function isStudentFormField(fieldName: string): fieldName is keyof StudentFormValues {
  return Object.prototype.hasOwnProperty.call(defaults, fieldName)
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">{title}</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-4 md:grid-cols-2">{children}</CardContent>
    </Card>
  )
}
