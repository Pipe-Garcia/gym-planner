import { Copy, Eye, RotateCcw, Trash2 } from "lucide-react"
import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import type { TemplateSummary } from "@/types/training"

interface Props { template: TemplateSummary; onDuplicate: () => void; onDeactivate: () => void; onReactivate: () => void }

export function TemplateCard({ template, onDuplicate, onDeactivate, onReactivate }: Props) {
  return (
    <Card>
      <CardContent className="space-y-4 p-4">
        <div>
          <div className="flex items-start justify-between gap-2"><h2 className="font-semibold">{template.name}</h2><span className={template.active ? "rounded-full bg-emerald-100 px-2 py-1 text-xs text-emerald-700" : "rounded-full bg-slate-100 px-2 py-1 text-xs text-slate-600"}>{template.active ? "Activa" : "Inactiva"}</span></div>
          <p className="mt-1 line-clamp-2 text-sm text-muted-foreground">{template.description || "Sin descripcion"}</p>
        </div>
        <div className="flex flex-wrap gap-2 text-xs text-muted-foreground"><span>{template.sport || "Sin deporte"}</span><span>{template.objective || "Sin objetivo"}</span><span>{template.level || "Sin nivel"}</span><span>{template.dayCount} días · {template.blockCount} bloques · {template.exerciseCount} ejercicios</span></div>
        <div className="flex flex-wrap gap-2">
          <Button asChild size="sm"><Link to={`/templates/${template.id}`}><Eye className="h-4 w-4" />Ver</Link></Button>
          <Button type="button" size="sm" variant="outline" onClick={onDuplicate}><Copy className="h-4 w-4" />Duplicar</Button>
          <Button type="button" size="sm" variant="outline" onClick={template.active ? onDeactivate : onReactivate}>{template.active ? <Trash2 className="h-4 w-4" /> : <RotateCcw className="h-4 w-4" />}{template.active ? "Desactivar" : "Reactivar"}</Button>
        </div>
      </CardContent>
    </Card>
  )
}
