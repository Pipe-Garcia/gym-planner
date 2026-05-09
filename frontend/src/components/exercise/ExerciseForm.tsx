import { zodResolver } from "@hookform/resolvers/zod"
import type { AxiosError } from "axios"
import { Save } from "lucide-react"
import { useEffect } from "react"
import { useForm } from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { TagMultiSelect } from "@/components/exercise/TagMultiSelect"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useToast } from "@/hooks/useToast"
import { measurementTypeOptions } from "@/lib/format"
import { exerciseFormSchema, type ExerciseFormValues } from "@/schemas/exercise.schema"
import type { ApiError } from "@/types/api"
import type { Exercise, ExerciseTag } from "@/types/exercise"

interface ExerciseFormProps {
  tags: ExerciseTag[]
  initialData?: Exercise
  onSubmit: (values: ExerciseFormValues) => Promise<Exercise>
  submitLabel: string
}

export function ExerciseForm({ tags, initialData, onSubmit, submitLabel }: ExerciseFormProps) {
  const toast = useToast()
  const navigate = useNavigate()
  const form = useForm<ExerciseFormValues>({
    resolver: zodResolver(exerciseFormSchema),
    defaultValues: {
      name: "",
      description: "",
      technicalNotes: "",
      defaultMeasurement: "REPS_WEIGHT",
      videoUrl: "",
      imageUrl: "",
      tagIds: [],
    },
  })

  useEffect(() => {
    if (initialData) {
      form.reset({
        name: initialData.name,
        description: initialData.description ?? "",
        technicalNotes: initialData.technicalNotes ?? "",
        defaultMeasurement: initialData.defaultMeasurement,
        videoUrl: initialData.videoUrl ?? "",
        imageUrl: initialData.imageUrl ?? "",
        tagIds: initialData.tags.map((tag) => tag.id),
      })
    }
  }, [form, initialData])

  async function handleSubmit(values: ExerciseFormValues) {
    try {
      const saved = await onSubmit(values)
      toast.success("Ejercicio guardado.")
      navigate(`/exercises/${saved.id}`)
    } catch (error) {
      const apiError = error as AxiosError<ApiError>
      if (apiError.response?.status === 409) {
        form.setError("name", { message: apiError.response.data.message })
      }
      toast.error(apiError.response?.data.message ?? "No pudimos guardar el ejercicio.")
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-5">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Datos del ejercicio</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-2">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nombre</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="defaultMeasurement"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Medición</FormLabel>
                  <FormControl>
                    <select className="h-11 w-full rounded-md border border-input bg-white px-3 text-sm" {...field}>
                      {measurementTypeOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            {renderTextArea("description", "Descripción")}
            {renderTextArea("technicalNotes", "Notas técnicas")}
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Tags</CardTitle>
          </CardHeader>
          <CardContent>
            <FormField
              control={form.control}
              name="tagIds"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <TagMultiSelect tags={tags} value={field.value ?? []} onChange={field.onChange} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>
        <details className="rounded-md border bg-white p-4">
          <summary className="cursor-pointer font-semibold">Multimedia</summary>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <FormField
              control={form.control}
              name="videoUrl"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Video URL</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="imageUrl"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Imagen URL</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </div>
        </details>
        <Button type="submit" disabled={form.formState.isSubmitting}>
          <Save className="h-4 w-4" />
          {form.formState.isSubmitting ? "Guardando..." : submitLabel}
        </Button>
      </form>
    </Form>
  )

  function renderTextArea(name: "description" | "technicalNotes", label: string) {
    return (
      <FormField
        key={name}
        control={form.control}
        name={name}
        render={({ field }) => (
          <FormItem className="md:col-span-2">
            <FormLabel>{label}</FormLabel>
            <FormControl>
              <textarea className="min-h-28 w-full rounded-md border border-input bg-white px-3 py-2 text-sm" {...field} />
            </FormControl>
            <FormMessage />
          </FormItem>
        )}
      />
    )
  }
}
