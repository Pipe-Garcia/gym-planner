import { zodResolver } from "@hookform/resolvers/zod"
import { Send } from "lucide-react"
import { useForm } from "react-hook-form"
import { noteFormSchema, type NoteFormValues } from "@/schemas/student.schema"
import { Button } from "@/components/ui/button"
import { Form, FormControl, FormField, FormItem, FormMessage } from "@/components/ui/form"

interface NoteFormProps {
  onSubmit: (values: NoteFormValues) => Promise<void>
}

export function NoteForm({ onSubmit }: NoteFormProps) {
  const form = useForm<NoteFormValues>({
    resolver: zodResolver(noteFormSchema),
    defaultValues: { content: "" },
  })

  async function handleSubmit(values: NoteFormValues) {
    await onSubmit(values)
    form.reset()
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-3 rounded-md border bg-white p-4">
        <p className="text-sm font-medium text-muted-foreground">Notas internas - solo visibles para el equipo</p>
        <FormField
          control={form.control}
          name="content"
          render={({ field }) => (
            <FormItem>
              <FormControl>
                <textarea className="min-h-28 w-full rounded-md border border-input bg-white px-3 py-2 text-sm" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit" disabled={form.formState.isSubmitting}>
          <Send className="h-4 w-4" />
          Agregar nota
        </Button>
      </form>
    </Form>
  )
}
