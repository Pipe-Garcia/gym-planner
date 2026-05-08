import { LoaderCircle } from "lucide-react"
import { cn } from "@/lib/utils"

export function LoadingSpinner({ className }: { className?: string }) {
  return <LoaderCircle className={cn("h-6 w-6 animate-spin text-primary", className)} aria-label="Cargando" />
}
