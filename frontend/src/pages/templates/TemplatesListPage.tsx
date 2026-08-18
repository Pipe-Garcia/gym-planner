import { FileStack, Plus } from "lucide-react"
import { useState } from "react"
import { Link } from "react-router-dom"
import { ConfirmDialog } from "@/components/shared/ConfirmDialog"
import { TemplateList } from "@/components/template/TemplateList"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { useDeactivateTemplate, useDuplicateTemplate, useReactivateTemplate, useTemplates } from "@/hooks/useTemplates"
import { useToast } from "@/hooks/useToast"
import type { TemplateSummary } from "@/types/training"

type PendingTemplateAction = {
  id: number
  action: "duplicate" | "deactivate" | "reactivate"
} | null

export function TemplatesListPage() {
  const toast = useToast()
  const [search, setSearch] = useState("")
  const [showInactive, setShowInactive] = useState(false)
  const [pendingAction, setPendingAction] = useState<PendingTemplateAction>(null)
  const [templateToDeactivate, setTemplateToDeactivate] = useState<TemplateSummary | null>(null)
  const query = useTemplates({ search: search || undefined, active: showInactive ? false : true, page: 0, size: 30, sort: "name,asc" })
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
    const template = query.data?.content.find((item) => item.id === id)
    if (template) setTemplateToDeactivate(template)
  }

  async function confirmDeactivateTemplate() {
    if (!templateToDeactivate) return
    setPendingAction({ id: templateToDeactivate.id, action: "deactivate" })
    try {
      await deactivate.mutateAsync(templateToDeactivate.id)
      toast.success("Plantilla desactivada.")
      setTemplateToDeactivate(null)
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
      {query.isLoading ? (
        <TemplatesListSkeleton />
      ) : query.data?.content.length ? (
        <TemplateList templates={query.data.content} onDuplicate={onDuplicate} onDeactivate={onDeactivate} onReactivate={onReactivate} pendingAction={pendingAction} />
      ) : (
        <div className="rounded-md border bg-white p-8 text-center text-sm text-muted-foreground"><FileStack className="mx-auto mb-2 h-8 w-8" />No hay plantillas.</div>
      )}
      <ConfirmDialog
        open={Boolean(templateToDeactivate)}
        onOpenChange={(open) => {
          if (!open) setTemplateToDeactivate(null)
        }}
        title="Desactivar plantilla"
        description="La plantilla dejará de estar disponible para crear nuevas rutinas. Podés reactivarla más adelante."
        confirmLabel="Desactivar"
        loadingLabel="Desactivando..."
        isPending={deactivate.isPending}
        onConfirm={() => {
          void confirmDeactivateTemplate()
        }}
      />
    </div>
  )
}

function TemplatesListSkeleton() {
  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {Array.from({ length: 6 }).map((_, index) => (
        <div key={index} className="rounded-md border bg-white p-4">
          <div className="flex items-start justify-between gap-2">
            <Skeleton className="h-5 w-40" />
            <Skeleton className="h-6 w-16 rounded-full" />
          </div>
          <Skeleton className="mt-3 h-4 w-full" />
          <Skeleton className="mt-2 h-4 w-2/3" />
          <div className="mt-4 flex flex-wrap gap-2">
            <Skeleton className="h-4 w-20" />
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-4 w-16" />
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            <Skeleton className="h-9 w-16" />
            <Skeleton className="h-9 w-24" />
            <Skeleton className="h-9 w-28" />
          </div>
        </div>
      ))}
    </div>
  )
}
