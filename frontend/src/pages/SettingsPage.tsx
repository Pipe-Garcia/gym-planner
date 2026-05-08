import { zodResolver } from "@hookform/resolvers/zod"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Save } from "lucide-react"
import { useEffect } from "react"
import { useForm } from "react-hook-form"
import { z } from "zod"
import { getCurrentGym, updateCurrentGym } from "@/api/gym"
import { LoadingSpinner } from "@/components/shared/LoadingSpinner"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useToast } from "@/hooks/useToast"

const gymSchema = z.object({
  name: z.string().min(1, "El nombre es obligatorio.").max(150),
  ownerName: z.string().max(150).optional(),
  phone: z.string().max(50).optional(),
  email: z.union([z.string().email("Ingresá un email válido."), z.literal("")]).optional(),
  address: z.string().max(255).optional(),
  primaryColor: z
    .union([z.string().regex(/^#[0-9A-Fa-f]{6}$/, "Usá formato hexadecimal, por ejemplo #2563EB."), z.literal("")])
    .optional(),
  logoUrl: z.string().max(500).optional(),
})

type GymFormValues = z.infer<typeof gymSchema>

export function SettingsPage() {
  const toast = useToast()
  const queryClient = useQueryClient()
  const form = useForm<GymFormValues>({
    resolver: zodResolver(gymSchema),
    defaultValues: {
      name: "",
      ownerName: "",
      phone: "",
      email: "",
      address: "",
      primaryColor: "#2563EB",
      logoUrl: "",
    },
  })

  const gymQuery = useQuery({
    queryKey: ["gym", "current"],
    queryFn: getCurrentGym,
  })

  useEffect(() => {
    if (gymQuery.data) {
      form.reset({
        name: gymQuery.data.name,
        ownerName: gymQuery.data.ownerName ?? "",
        phone: gymQuery.data.phone ?? "",
        email: gymQuery.data.email ?? "",
        address: gymQuery.data.address ?? "",
        primaryColor: gymQuery.data.primaryColor ?? "#2563EB",
        logoUrl: gymQuery.data.logoUrl ?? "",
      })
    }
  }, [form, gymQuery.data])

  const mutation = useMutation({
    mutationFn: updateCurrentGym,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["gym", "current"] })
      toast.success("Configuración guardada.")
    },
    onError: () => {
      toast.error("No pudimos guardar la configuración.")
    },
  })

  function emptyToUndefined(value?: string) {
    return value?.trim() ? value.trim() : undefined
  }

  function onSubmit(values: GymFormValues) {
    mutation.mutate({
      name: values.name.trim(),
      ownerName: emptyToUndefined(values.ownerName),
      phone: emptyToUndefined(values.phone),
      email: emptyToUndefined(values.email),
      address: emptyToUndefined(values.address),
      primaryColor: emptyToUndefined(values.primaryColor),
      logoUrl: emptyToUndefined(values.logoUrl),
    })
  }

  if (gymQuery.isLoading) {
    return (
      <div className="flex min-h-80 items-center justify-center">
        <LoadingSpinner />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-normal">Configuración</h1>
        <p className="mt-1 text-sm text-muted-foreground">Datos visibles del gimnasio.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Gimnasio</CardTitle>
          <CardDescription>Actualizá la información principal del espacio.</CardDescription>
        </CardHeader>
        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="grid gap-4 md:grid-cols-2">
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
                name="ownerName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Dueño / responsable</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="phone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Teléfono</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Email</FormLabel>
                    <FormControl>
                      <Input type="email" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="address"
                render={({ field }) => (
                  <FormItem className="md:col-span-2">
                    <FormLabel>Dirección</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="primaryColor"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Color principal</FormLabel>
                    <div className="flex gap-3">
                      <FormControl>
                        <Input {...field} />
                      </FormControl>
                      <Input
                        type="color"
                        value={field.value || "#2563EB"}
                        onChange={field.onChange}
                        className="w-14 shrink-0 p-1"
                        aria-label="Selector de color"
                      />
                    </div>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="logoUrl"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Logo URL</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <div className="md:col-span-2">
                <Button type="submit" disabled={mutation.isPending}>
                  <Save className="h-4 w-4" />
                  {mutation.isPending ? "Guardando..." : "Guardar"}
                </Button>
              </div>
            </form>
          </Form>
        </CardContent>
      </Card>
    </div>
  )
}
