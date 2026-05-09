import { Plus, XCircle } from "lucide-react"
import { Button } from "@/components/ui/button"
import type { StudentInjury } from "@/types/student"

interface InjuryListProps {
  injuries: StudentInjury[]
  onAdd: () => void
  onResolve: (injury: StudentInjury) => void
}

export function InjuryList({ injuries, onAdd, onResolve }: InjuryListProps) {
  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-semibold">Lesiones</h2>
        <Button type="button" onClick={onAdd}>
          <Plus className="h-4 w-4" />
          Agregar lesión
        </Button>
      </div>
      {injuries.length === 0 ? (
        <div className="rounded-md border bg-white p-6 text-sm text-muted-foreground">No hay lesiones activas cargadas.</div>
      ) : (
        <div className="grid gap-3">
          {injuries.map((injury) => (
            <article key={injury.id} className="rounded-md border bg-white p-4">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h3 className="font-semibold">{injury.bodyArea}</h3>
                  <p className="mt-1 text-sm text-muted-foreground">{injury.severity}</p>
                </div>
                <Button type="button" variant="ghost" size="icon" onClick={() => onResolve(injury)} aria-label="Resolver lesión">
                  <XCircle className="h-4 w-4" />
                </Button>
              </div>
              <p className="mt-3 text-sm">{injury.description}</p>
              {injury.notes ? <p className="mt-2 text-sm text-muted-foreground">{injury.notes}</p> : null}
            </article>
          ))}
        </div>
      )}
    </section>
  )
}
