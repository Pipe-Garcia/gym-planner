import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { injuryFormSchema, type InjuryFormValues } from "@/schemas/student.schema"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"

interface InjuryFormProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (values: InjuryFormValues) => Promise<void>
}

export function InjuryForm({ open, onOpenChange, onSubmit }: InjuryFormProps) {
  const form = useForm<InjuryFormValues>({
    resolver: zodResolver(injuryFormSchema),
    defaultValues: { bodyArea: "", description: "", severity: "LEVE", startedAt: "", notes: "" },
  })

  async function handleSubmit(values: InjuryFormValues) {
    await onSubmit(values)
    form.reset()
    onOpenChange(false)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Agregar lesión</DialogTitle>
          <DialogDescription>Esta informacion NO se incluye en PDFs ni mensajes al alumno.</DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="bodyArea"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Zona del cuerpo</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="severity"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Severidad</FormLabel>
                  <FormControl>
                    <select className="h-11 w-full rounded-md border border-input bg-white px-3 text-sm" {...field}>
                      <option value="LEVE">Leve</option>
                      <option value="MODERADA">Moderada</option>
                      <option value="GRAVE">Grave</option>
                    </select>
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="startedAt"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Inicio</FormLabel>
                  <FormControl>
                    <Input type="date" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Descripción</FormLabel>
                  <FormControl>
                    <textarea className="min-h-24 w-full rounded-md border border-input bg-white px-3 py-2 text-sm" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="notes"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Notas</FormLabel>
                  <FormControl>
                    <textarea className="min-h-20 w-full rounded-md border border-input bg-white px-3 py-2 text-sm" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <Button type="submit" disabled={form.formState.isSubmitting}>
              Guardar lesión
            </Button>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
