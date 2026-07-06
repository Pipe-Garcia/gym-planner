import { FileStack, Plus } from "lucide-react"
import { useState } from "react"
import { Link } from "react-router-dom"
import { TemplateList } from "@/components/template/TemplateList"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useDeactivateTemplate, useDuplicateTemplate, useReactivateTemplate, useTemplates } from "@/hooks/useTemplates"
import { useToast } from "@/hooks/useToast"

type PendingTemplateAction = {
  id: number
  action: "duplicate" | "deactivate" | "reactivate"
} | null

export function TemplatesListPage() {
  const toast = useToast()
  const [search, setSearch] = useState("")
  const [showInactive, setShowInactive] = useState(false)
  const [pendingAction, setPendingAction] = useState<PendingTemplateAction>(null)
  const query = useTemplates({ search: search || undefined, active: showInactive ? undefined : true, page: 0, size: 30, sort: "name,asc" })
  const duplicate = useDuplicateTemplate()
  const deactivate = useDeactivateTemplate()
  const reactivate = useReactivateTemplate()

  async function onDuplicate(id: number) {
    setPendingAction({ id, action: "duplicate" })
    try {
      await duplicate.mutateAsync(id)
      toast.success("Plantilla duplicada.")
    } finally {
      setPendingAction(null)
    }
  }

  async function onDeactivate(id: number) {
    setPendingAction({ id, action: "deactivate" })
    try {
      await deactivate.mutateAsync(id)
      toast.success("Plantilla desactivada.")
    } finally {
      setPendingAction(null)
    }
  }

  async function onReactivate(id: number) {
    setPendingAction({ id, action: "reactivate" })
    try {
      await reactivate.mutateAsync(id)
      toast.success("Plantilla reactivada.")
    } finally {
      setPendingAction(null)
    }
  }

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
      {query.data?.content.length ? <TemplateList templates={query.data.content} onDuplicate={onDuplicate} onDeactivate={onDeactivate} onReactivate={onReactivate} pendingAction={pendingAction} /> : <div className="rounded-md border bg-white p-8 text-center text-sm text-muted-foreground"><FileStack className="mx-auto mb-2 h-8 w-8" />No hay plantillas.</div>}
    </div>
  )
}
