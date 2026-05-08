import { zodResolver } from "@hookform/resolvers/zod"
import { Dumbbell } from "lucide-react"
import { useEffect } from "react"
import { useForm } from "react-hook-form"
import { useNavigate } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { useAuth } from "@/hooks/useAuth"
import { useToast } from "@/hooks/useToast"
import { loginSchema, type LoginFormValues } from "@/schemas/auth.schema"

export function LoginPage() {
  const navigate = useNavigate()
  const toast = useToast()
  const { login, isAuthenticated } = useAuth()
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "admin@gymplanner.local",
      password: "",
    },
  })

  useEffect(() => {
    if (isAuthenticated) {
      navigate("/", { replace: true })
    }
  }, [isAuthenticated, navigate])

  async function onSubmit(values: LoginFormValues) {
    try {
      await login(values.email, values.password)
      navigate("/", { replace: true })
    } catch {
      toast.error("No pudimos iniciar sesión. Revisá email y password.")
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-background px-4 py-8">
      <div className="w-full max-w-md">
        <div className="mb-6 flex items-center justify-center gap-3">
          <span className="flex h-12 w-12 items-center justify-center rounded-md bg-primary text-primary-foreground">
            <Dumbbell className="h-6 w-6" />
          </span>
          <div>
            <h1 className="text-xl font-semibold tracking-normal">Gym Planner</h1>
            <p className="text-sm text-muted-foreground">Acceso al panel</p>
          </div>
        </div>
        <Card>
          <CardHeader>
            <CardTitle>Iniciar sesión</CardTitle>
            <CardDescription>Ingresá con tu usuario del gimnasio.</CardDescription>
          </CardHeader>
          <CardContent>
            <Form {...form}>
              <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                <FormField
                  control={form.control}
                  name="email"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Email</FormLabel>
                      <FormControl>
                        <Input type="email" autoComplete="email" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <FormField
                  control={form.control}
                  name="password"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Password</FormLabel>
                      <FormControl>
                        <Input type="password" autoComplete="current-password" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
                <Button type="submit" className="w-full" disabled={form.formState.isSubmitting}>
                  {form.formState.isSubmitting ? "Ingresando..." : "Ingresar"}
                </Button>
              </form>
            </Form>
          </CardContent>
        </Card>
      </div>
    </main>
  )
}
