import { zodResolver } from "@hookform/resolvers/zod"
import type { AxiosError } from "axios"
import { Loader2 } from "lucide-react"
import { useEffect, useState } from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useCreateExerciseTag, useUpdateExerciseTag } from "@/hooks/useExercises"
import { useToast } from "@/hooks/useToast"
import type { ApiError } from "@/types/api"
import type { ExerciseTagUsage } from "@/types/exercise"
import { tagTypeLabels, tagTypeOrder } from "@/components/exercise/tag-admin/tagAdminLabels"

const tagFormSchema = z.object({
  name: z.string().min(1, "El nombre es obligatorio.").max(100, "El nombre no puede superar 100 caracteres."),
  type: z.enum(tagTypeOrder, "Seleccioná un tipo."),
})

type TagFormValues = z.infer<typeof tagFormSchema>

interface TagFormDialogProps {
  open: boolean
  tag?: ExerciseTagUsage | null
  onOpenChange: (open: boolean) => void
}

export function TagFormDialog({ open, tag, onOpenChange }: TagFormDialogProps) {
  const isEditing = Boolean(tag)
  const toast = useToast()
  const createTag = useCreateExerciseTag()
  const updateTag = useUpdateExerciseTag()
  const [formError, setFormError] = useState<string | null>(null)
  const form = useForm<TagFormValues>({
    resolver: zodResolver(tagFormSchema),
    defaultValues: {
      name: "",
      type: "BODY_AREA",
    },
  })

  useEffect(() => {
    if (!open) return
    setFormError(null)
    form.reset({
      name: tag?.name ?? "",
      type: tag?.type ?? "BODY_AREA",
    })
  }, [form, open, tag])

  async function onSubmit(values: TagFormValues) {
    setFormError(null)
    const name = values.name.trim()
    try {
      if (tag) {
        await updateTag.mutateAsync({ id: tag.id, data: { name } })
        toast.success("Etiqueta actualizada.")
      } else {
        await createTag.mutateAsync({ name, type: values.type })
        toast.success("Etiqueta creada.")
      }
      onOpenChange(false)
    } catch (error) {
      const apiError = error as AxiosError<ApiError>
      if (apiError.response?.status === 409) {
        form.setError("name", { message: "Ya existe una etiqueta de ese tipo con ese nombre." })
        return
      }
      if (apiError.response?.status === 404) {
        setFormError("No encontramos esa etiqueta. Puede haber sido eliminada.")
        return
      }
      if (apiError.response?.status === 400) {
        setFormError(apiError.response.data.message ?? "Revisá los datos ingresados.")
        return
      }
      setFormError("No pudimos guardar la etiqueta.")
    }
  }

  const isPending = createTag.isPending || updateTag.isPending

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEditing ? "Editar etiqueta" : "Nueva etiqueta"}</DialogTitle>
          <DialogDescription>
            {isEditing ? "Actualizá el nombre de la etiqueta." : "Creá una etiqueta para clasificar tus ejercicios."}
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Nombre</FormLabel>
                  <FormControl>
                    <Input {...field} autoFocus />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="type"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Tipo</FormLabel>
                  <FormControl>
                    <select
                      className="h-11 w-full rounded-md border border-input bg-white px-3 text-sm disabled:cursor-not-allowed disabled:opacity-70"
                      disabled={isEditing}
                      {...field}
                    >
                      {tagTypeOrder.map((type) => (
                        <option key={type} value={type}>
                          {tagTypeLabels[type]}
                        </option>
                      ))}
                    </select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            {formError ? <p className="text-sm font-medium text-destructive">{formError}</p> : null}
            <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancelar
              </Button>
              <Button type="submit" disabled={isPending}>
                {isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                {isPending ? "Guardando..." : isEditing ? "Guardar" : "Crear"}
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
