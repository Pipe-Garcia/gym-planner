import { Copy, Loader2, Pencil, RotateCcw, Trash2 } from "lucide-react"
import { Link, useNavigate, useParams } from "react-router-dom"
import { BackButton } from "@/components/shared/BackButton"
import { TrainingPlanReadOnlyView } from "@/components/template/TrainingPlanReadOnlyView"
import { Button } from "@/components/ui/button"
import { useDeactivateTemplate, useDuplicateTemplate, useReactivateTemplate, useTemplate } from "@/hooks/useTemplates"
import { useToast } from "@/hooks/useToast"

const actionButtonClass = "h-8 gap-1.5 px-2.5 text-xs"
const actionIconClass = "h-3.5 w-3.5"

export function TemplateDetailPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const toast = useToast()
  const templateQuery = useTemplate(id)
  const duplicate = useDuplicateTemplate()
  const deactivate = useDeactivateTemplate()
  const reactivate = useReactivateTemplate()
  const template = templateQuery.data
  const isTogglePending = deactivate.isPending || reactivate.isPending

  if (templateQuery.isLoading) return <div className="rounded-md border bg-white p-6 text-sm text-muted-foreground">Cargando plantilla...</div>
  if (!template) return <div className="rounded-md border bg-white p-6 text-sm text-muted-foreground">Plantilla no encontrada.</div>

  async function onDuplicate() {
    const copy = await duplicate.mutateAsync(id)
    toast.success("Plantilla duplicada.")
    navigate(`/templates/${copy.id}/edit`)
  }

  async function onToggleActive() {
    if (template?.active) {
      await deactivate.mutateAsync(id)
      toast.success("Plantilla desactivada.")
    } else {
      await reactivate.mutateAsync(id)
      toast.success("Plantilla reactivada.")
    }
    await templateQuery.refetch()
  }

  return (
    <div className="space-y-6">
      <BackButton to="/templates" />
      <div className="flex flex-col gap-4 rounded-md border bg-white p-4 lg:flex-row lg:items-start lg:justify-between">
        <div className="space-y-3">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold leading-tight tracking-normal">{template.name}</h1>
            <span className={template.active ? "inline-flex items-center rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-medium leading-none text-emerald-700" : "inline-flex items-center rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium leading-none text-slate-600"}>
              {template.active ? "Activa" : "Inactiva"}
            </span>
          </div>
          <div className="grid gap-2 text-sm text-muted-foreground sm:grid-cols-3">
            <Meta label="Deporte" value={template.sport} />
            <Meta label="Objetivo" value={template.objective} />
            <Meta label="Nivel" value={template.level} />
          </div>
          {template.description ? <p className="max-w-3xl text-sm">{template.description}</p> : null}
          {template.generalNotes ? (
            <div className="max-w-3xl rounded-md bg-muted/40 p-3 text-sm">
              <p className="font-medium">Notas generales</p>
              <p className="mt-1 text-muted-foreground">{template.generalNotes}</p>
            </div>
          ) : null}
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Button asChild size="sm" className={actionButtonClass}>
            <Link to={`/templates/${template.id}/edit`}><Pencil className={actionIconClass} />Editar</Link>
          </Button>
          <Button type="button" size="sm" variant="outline" className={actionButtonClass} onClick={onDuplicate} disabled={duplicate.isPending}>
            {duplicate.isPending ? <Loader2 className={`${actionIconClass} animate-spin`} /> : <Copy className={actionIconClass} />}
            {duplicate.isPending ? "Duplicando..." : "Duplicar"}
          </Button>
          <Button type="button" size="sm" variant="outline" className={actionButtonClass} onClick={onToggleActive} disabled={isTogglePending}>
            {isTogglePending ? <Loader2 className={`${actionIconClass} animate-spin`} /> : template.active ? <Trash2 className={actionIconClass} /> : <RotateCcw className={actionIconClass} />}
            {isTogglePending ? (template.active ? "Desactivando..." : "Reactivando...") : template.active ? "Desactivar" : "Reactivar"}
          </Button>
        </div>
      </div>
      <TrainingPlanReadOnlyView days={template.days} context="template" />
    </div>
  )
}

function Meta({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <p className="text-xs uppercase text-muted-foreground">{label}</p>
      <p className="text-sm text-foreground">{value || "-"}</p>
    </div>
  )
}
