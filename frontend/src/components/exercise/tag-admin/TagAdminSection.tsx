import type { AxiosError } from "axios"
import { ChevronDown, Pencil, Plus, Trash2 } from "lucide-react"
import { useState } from "react"
import { TagBadge } from "@/components/exercise/TagBadge"
import { TagFormDialog } from "@/components/exercise/tag-admin/TagFormDialog"
import { tagTypeLabels, tagTypeOrder } from "@/components/exercise/tag-admin/tagAdminLabels"
import { ConfirmDialog } from "@/components/shared/ConfirmDialog"
import { LoadingSpinner } from "@/components/shared/LoadingSpinner"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { useDeleteExerciseTag, useExerciseTagUsage } from "@/hooks/useExercises"
import { useToast } from "@/hooks/useToast"
import type { ApiError } from "@/types/api"
import type { ExerciseTagUsage, TagType } from "@/types/exercise"

const tagTypeColorDots: Record<TagType, string> = {
  BODY_AREA: "bg-blue-500",
  MUSCLE_GROUP: "bg-indigo-500",
  MOVEMENT_PATTERN: "bg-violet-500",
  OBJECTIVE: "bg-emerald-500",
  LEVEL: "bg-amber-500",
  EQUIPMENT: "bg-slate-500",
}

export function TagAdminSection() {
  const toast = useToast()
  const tagsQuery = useExerciseTagUsage()
  const deleteTag = useDeleteExerciseTag()
  const [formOpen, setFormOpen] = useState(false)
  const [editingTag, setEditingTag] = useState<ExerciseTagUsage | null>(null)
  const [deletingTag, setDeletingTag] = useState<ExerciseTagUsage | null>(null)

  function openCreateDialog() {
    setEditingTag(null)
    setFormOpen(true)
  }

  function openEditDialog(tag: ExerciseTagUsage) {
    setEditingTag(tag)
    setFormOpen(true)
  }

  async function confirmDelete() {
    if (!deletingTag) return
    try {
      await deleteTag.mutateAsync(deletingTag.id)
      toast.success("Etiqueta eliminada.")
      setDeletingTag(null)
    } catch (error) {
      const apiError = error as AxiosError<ApiError>
      toast.error(apiError.response?.data.message ?? "No pudimos eliminar la etiqueta.")
    }
  }

  const tags = tagsQuery.data ?? []
  const grouped = groupTagsByType(tags)

  return (
    <Card>
      <CardHeader className="gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <CardTitle>Etiquetas de ejercicios</CardTitle>
          <CardDescription>Organizá las etiquetas con las que clasificás y filtrás tus ejercicios.</CardDescription>
        </div>
        <Button type="button" className="w-full sm:w-auto" onClick={openCreateDialog}>
          <Plus className="h-4 w-4" />
          Nueva etiqueta
        </Button>
      </CardHeader>
      <CardContent>
        {tagsQuery.isLoading ? (
          <div className="flex min-h-40 items-center justify-center">
            <LoadingSpinner />
          </div>
        ) : tagsQuery.isError ? (
          <div className="rounded-md border border-destructive/30 bg-destructive/5 p-4 text-sm text-destructive">
            No pudimos cargar las etiquetas.
          </div>
        ) : tags.length === 0 ? (
          <div className="rounded-md border bg-muted/30 p-6 text-center text-sm text-muted-foreground">
            Todavía no hay etiquetas de ejercicios.
          </div>
        ) : (
          <div className="space-y-3">
            {tagTypeOrder.map((type) => (
              <TagTypeGroup
                key={type}
                type={type}
                tags={grouped[type]}
                onEdit={openEditDialog}
                onDelete={setDeletingTag}
              />
            ))}
          </div>
        )}
      </CardContent>
      <TagFormDialog open={formOpen} tag={editingTag} onOpenChange={setFormOpen} />
      <ConfirmDialog
        open={Boolean(deletingTag)}
        title={deletingTag ? `Eliminar etiqueta «${deletingTag.name}»` : "Eliminar etiqueta"}
        description={deleteDescription(deletingTag)}
        confirmLabel="Eliminar etiqueta"
        onOpenChange={(open) => {
          if (!open) setDeletingTag(null)
        }}
        onConfirm={() => {
          void confirmDelete()
        }}
      />
    </Card>
  )
}

interface TagTypeGroupProps {
  type: TagType
  tags: ExerciseTagUsage[]
  onEdit: (tag: ExerciseTagUsage) => void
  onDelete: (tag: ExerciseTagUsage) => void
}

function TagTypeGroup({ type, tags, onEdit, onDelete }: TagTypeGroupProps) {
  return (
    <details className="group rounded-md border bg-white">
      <summary className="flex min-h-14 cursor-pointer list-none items-center justify-between gap-3 px-3 py-2 text-sm font-semibold">
        <span className="flex min-w-0 items-center gap-2">
          <span className={`h-2.5 w-2.5 shrink-0 rounded-full ${tagTypeColorDots[type]}`} />
          <span className="truncate">{tagTypeLabels[type]}</span>
        </span>
        <span className="flex shrink-0 items-center gap-2">
          <span className="rounded-full bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">
            {tagCountText(tags.length)}
          </span>
          <ChevronDown className="h-4 w-4 text-muted-foreground transition-transform group-open:rotate-180" />
        </span>
      </summary>
      {tags.length ? (
        <div className="grid gap-3 border-t p-3 md:grid-cols-2">
          {tags.map((tag) => (
            <div key={tag.id} className="flex min-w-0 items-center gap-2 overflow-hidden rounded-md border bg-muted/20 p-3">
              <div className="flex min-w-0 flex-1 items-center gap-2 overflow-hidden">
                <TagBadge tag={tag} />
                <span className={tag.usageCount > 0 ? "min-w-0 truncate text-sm text-muted-foreground" : "min-w-0 truncate text-sm text-muted-foreground/80"}>
                  {usageText(tag.usageCount)}
                </span>
              </div>
              <div className="flex shrink-0 items-center gap-1">
                <Button type="button" variant="outline" size="icon" onClick={() => onEdit(tag)} aria-label={`Editar etiqueta ${tag.name}`}>
                  <Pencil className="h-4 w-4" />
                </Button>
                <Button type="button" variant="ghost" size="icon" onClick={() => onDelete(tag)} aria-label={`Eliminar etiqueta ${tag.name}`}>
                  <Trash2 className="h-4 w-4 text-destructive" />
                </Button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <p className="border-t p-3 text-sm text-muted-foreground">Sin etiquetas en este grupo.</p>
      )}
    </details>
  )
}

function groupTagsByType(tags: ExerciseTagUsage[]) {
  return tagTypeOrder.reduce<Record<TagType, ExerciseTagUsage[]>>((acc, type) => {
    acc[type] = tags.filter((tag) => tag.type === type)
    return acc
  }, {} as Record<TagType, ExerciseTagUsage[]>)
}

function usageText(usageCount: number) {
  if (usageCount === 0) return "Sin uso"
  if (usageCount === 1) return "Usado en 1 ejercicio"
  return `Usado en ${usageCount} ejercicios`
}

function tagCountText(count: number) {
  if (count === 1) return "1 etiqueta"
  return `${count} etiquetas`
}

function deleteDescription(tag: ExerciseTagUsage | null) {
  if (!tag) return ""
  if (tag.usageCount === 0) {
    return "Esta etiqueta no está usada por ningún ejercicio. ¿Querés eliminarla?"
  }
  const exerciseText = tag.usageCount === 1 ? "1 ejercicio" : `${tag.usageCount} ejercicios`
  return `Esta etiqueta está usada en ${exerciseText}. Si la eliminás, se quitará de esos ejercicios, pero no se borrará ningún ejercicio ni rutina.`
}
