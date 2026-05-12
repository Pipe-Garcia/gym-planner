import { FileStack, Plus } from "lucide-react"
import { useState } from "react"
import { Link } from "react-router-dom"
import { TemplateList } from "@/components/template/TemplateList"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useDeactivateTemplate, useDuplicateTemplate, useReactivateTemplate, useTemplates } from "@/hooks/useTemplates"
import { useToast } from "@/hooks/useToast"

export function TemplatesListPage() {
  const toast = useToast()
  const [search, setSearch] = useState("")
  const [showInactive, setShowInactive] = useState(false)
  const query = useTemplates({ search: search || undefined, active: showInactive ? undefined : true, page: 0, size: 30, sort: "name,asc" })
  const duplicate = useDuplicateTemplate()
  const deactivate = useDeactivateTemplate()
  const reactivate = useReactivateTemplate()
  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div><h1 className="text-2xl font-semibold tracking-normal">Plantillas</h1><p className="text-sm text-muted-foreground">Biblioteca de estructuras reutilizables.</p></div>
        <Button asChild><Link to="/templates/new"><Plus className="h-4 w-4" />Nueva plantilla</Link></Button>
      </div>
      <div className="flex flex-col gap-3 rounded-md border bg-white p-4 sm:flex-row sm:items-center">
        <Input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por nombre" />
        <label className="flex min-h-11 items-center gap-2 text-sm"><input type="checkbox" checked={showInactive} onChange={(event) => setShowInactive(event.target.checked)} />Mostrar inactivas</label>
      </div>
      {query.data?.content.length ? <TemplateList templates={query.data.content} onDuplicate={async (id) => { await duplicate.mutateAsync(id); toast.success("Plantilla duplicada.") }} onDeactivate={async (id) => { await deactivate.mutateAsync(id); toast.success("Plantilla desactivada.") }} onReactivate={async (id) => { await reactivate.mutateAsync(id); toast.success("Plantilla reactivada.") }} /> : <div className="rounded-md border bg-white p-8 text-center text-sm text-muted-foreground"><FileStack className="mx-auto mb-2 h-8 w-8" />No hay plantillas.</div>}
    </div>
  )
}