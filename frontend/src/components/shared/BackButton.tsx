import { ArrowLeft } from "lucide-react"
import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"

interface BackButtonProps {
  to: string
}

export function BackButton({ to }: BackButtonProps) {
  return (
    <Button type="button" variant="ghost" asChild>
      <Link to={to}>
        <ArrowLeft className="h-4 w-4" />
        Volver
      </Link>
    </Button>
  )
}
