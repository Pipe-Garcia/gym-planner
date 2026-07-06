import { StrictMode } from "react"
import type { CSSProperties } from "react"
import { createRoot } from "react-dom/client"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { BrowserRouter } from "react-router-dom"
import { Toaster } from "sonner"
import { CheckCircle2, CircleAlert, Info } from "lucide-react"
import { App } from "@/App"
import { AuthProvider } from "@/contexts/AuthContext"
import "@/index.css"

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
})

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <App />
          <Toaster
            position="bottom-center"
            duration={4000}
            offset={{ bottom: "1.75rem" }}
            mobileOffset={{ bottom: "1.5rem" }}
            icons={{
              success: <CheckCircle2 className="h-4 w-4" />,
              error: <CircleAlert className="h-4 w-4" />,
              info: <Info className="h-4 w-4" />,
            }}
            style={{ "--width": "304px" } as CSSProperties}
            toastOptions={{
              classNames: {
                toast: "gym-toast",
                success: "gym-toast-success",
                error: "gym-toast-error",
                info: "gym-toast-info",
                title: "gym-toast-title",
                icon: "gym-toast-icon",
              },
            }}
          />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
