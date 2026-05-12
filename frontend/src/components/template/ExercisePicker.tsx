import { Search, X } from "lucide-react"
import { useMemo, useState } from "react"
import { useExercises } from "@/hooks/useExercises"
import type { ExerciseSummary } from "@/types/exercise"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"

interface Props { open: boolean; onOpenChange: (open: boolean) => void; onSelect: (exercise: ExerciseSummary) => void }

export function ExercisePicker({ open, onOpenChange, onSelect }: Props) {
  const [search, setSearch] = useState("")
  const params = useMemo(() => ({ search: search || undefined, active: true, page: 0, size: 20, sort: "name,asc" }), [search])
  const query = useExercises(params)
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
        <DialogHeader><DialogTitle>Agregar ejercicio</DialogTitle></DialogHeader>
        <div className="relative">
          <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
          <Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar ejercicio" className="pl-9" />
          {search ? <button type="button" className="absolute right-3 top-3" onClick={() => setSearch("")}><X className="h-4 w-4" /></button> : null}
        </div>
        <div className="grid gap-2">
          {(query.data?.content ?? []).map((exercise) => (
            <button key={exercise.id} type="button" className="rounded-md border p-3 text-left hover:bg-muted" onClick={() => { onSelect(exercise); onOpenChange(false) }}>
              <p className="font-medium">{exercise.name}</p>
              <p className="mt-1 text-xs text-muted-foreground">{exercise.tags.map((tag) => tag.name).join(" · ") || "Sin tags"}</p>
            </button>
          ))}
          {!query.isLoading && query.data?.content.length === 0 ? <p className="py-8 text-center text-sm text-muted-foreground">No hay ejercicios activos para esa busqueda.</p> : null}
        </div>
        <div className="flex justify-end"><Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cerrar</Button></div>
      </DialogContent>
    </Dialog>
  )
}