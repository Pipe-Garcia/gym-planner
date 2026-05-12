import { Lock, MessageCircle } from "lucide-react"
import { useState } from "react"
import { useFormContext } from "react-hook-form"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"

interface Props {
  disabled?: boolean
}

export function RoutineNotesTabs({ disabled }: Props) {
  const { register } = useFormContext()
  const [activeTab, setActiveTab] = useState<"general" | "internal">("general")

  return (
    <div className="rounded-md border bg-background">
      <div className="flex border-b">
        <button
          type="button"
          className={cn(
            "flex flex-1 items-center justify-center gap-2 px-3 py-2 text-sm transition",
            activeTab === "general" ? "border-b-2 border-primary font-medium text-foreground" : "text-muted-foreground hover:text-foreground",
          )}
          onClick={() => setActiveTab("general")}
        >
          <MessageCircle className="h-4 w-4" />
          Para el alumno
        </button>
        <button
          type="button"
          className={cn(
            "flex flex-1 items-center justify-center gap-2 px-3 py-2 text-sm transition",
            activeTab === "internal" ? "border-b-2 border-primary font-medium text-foreground" : "text-muted-foreground hover:text-foreground",
          )}
          onClick={() => setActiveTab("internal")}
        >
          <Lock className="h-4 w-4" />
          Solo equipo
        </button>
      </div>

      <div className="p-3">
        <div className={activeTab === "general" ? "" : "hidden"}>
          <Textarea placeholder="Notas que verá el alumno" rows={3} disabled={disabled} {...register("generalNotes")} />
          <p className="mt-1 text-xs text-muted-foreground">Estas notas son visibles para el alumno cuando se le envía la rutina.</p>
        </div>
        <div className={activeTab === "internal" ? "" : "hidden"}>
          <Textarea placeholder="Observaciones privadas del equipo" rows={3} disabled={disabled} {...register("internalNotes")} />
          <p className="mt-1 text-xs text-muted-foreground">Solo visibles para el equipo. No aparecen en PDF ni WhatsApp.</p>
        </div>
      </div>
    </div>
  )
}
