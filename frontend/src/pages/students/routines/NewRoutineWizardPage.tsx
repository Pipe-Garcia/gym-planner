import { Calendar, FileStack, Loader2, Plus } from "lucide-react"
import { useState } from "react"
import { useNavigate, useParams } from "react-router-dom"
import { BackButton } from "@/components/shared/BackButton"
import { defaultDay } from "@/components/template/formDefaults"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { useCreateRoutineFromScratch, useCreateRoutineFromTemplate } from "@/hooks/useRoutines"
import { useTemplates } from "@/hooks/useTemplates"
import { useToast } from "@/hooks/useToast"

export function NewRoutineWizardPage() {
  const studentId = Number(useParams().studentId)
  const navigate = useNavigate()
  const toast = useToast()
  const [mode, setMode] = useState<"choose" | "template">("choose")
  const [search, setSearch] = useState("")
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null)
  const [name, setName] = useState("")
  const [assignedDate, setAssignedDate] = useState(new Date().toISOString().slice(0, 10))
  const templates = useTemplates({ search: search || undefined, active: true, page: 0, size: 20 })
  const fromTemplate = useCreateRoutineFromTemplate(studentId)
  const fromScratch = useCreateRoutineFromScratch(studentId)

  async function createBlank() {
    const routine = await fromScratch.mutateAsync({ name: "Nueva rutina", objective: null, assignedDate, generalNotes: null, internalNotes: null, status: "DRAFT", days: [defaultDay(1)] })
    navigate(`/students/${studentId}/routines/${routine.id}/edit`)
  }

  async function createFromSelected() {
    if (!selectedTemplateId) return
    const routine = await fromTemplate.mutateAsync({ studentId, templateId: selectedTemplateId, name: name || null, assignedDate, status: "ACTIVE" })
    toast.success("Rutina creada desde plantilla.")
    navigate(`/students/${studentId}/routines/${routine.id}/edit`)
  }

  return (
    <div className="space-y-6">
      <BackButton to={`/students/${studentId}`} />
      <h1 className="text-2xl font-semibold tracking-normal">Nueva rutina</h1>
      {mode === "choose" ? (
        <div className="grid gap-4 md:grid-cols-2">
          <Card><CardContent className="space-y-3 p-6"><FileStack className="h-8 w-8" /><h2 className="font-semibold">Desde plantilla</h2><p className="text-sm text-muted-foreground">Copia profunda de días, bloques, ejercicios y sets.</p><Button type="button" onClick={() => setMode("template")}>Elegir plantilla</Button></CardContent></Card>
          <Card><CardContent className="space-y-3 p-6"><Plus className="h-8 w-8" /><h2 className="font-semibold">Desde cero</h2><p className="text-sm text-muted-foreground">Crea un borrador editable sin afectar la rutina activa.</p><Button type="button" variant="outline" disabled={fromScratch.isPending} onClick={createBlank}>{fromScratch.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}{fromScratch.isPending ? "Creando..." : "Crear borrador"}</Button></CardContent></Card>
        </div>
      ) : null}
      {mode === "template" ? (
        <div className="space-y-4">
          <div className="grid gap-3 rounded-md border bg-white p-4 sm:grid-cols-3">
            <Input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Buscar plantilla" />
            <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Nombre opcional" />
            <label className="relative"><Calendar className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" /><Input type="date" value={assignedDate} onChange={(e) => setAssignedDate(e.target.value)} className="pl-9" /></label>
          </div>
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {templates.data?.content.map((template) => (
              <button key={template.id} type="button" onClick={() => { setSelectedTemplateId(template.id); setName(template.name) }} className={selectedTemplateId === template.id ? "rounded-md border border-primary bg-primary/5 p-4 text-left" : "rounded-md border bg-white p-4 text-left hover:bg-muted"}>
                <p className="font-medium">{template.name}</p>
                <p className="mt-1 text-sm text-muted-foreground">{template.dayCount} días · {template.blockCount} bloques · {template.exerciseCount} ejercicios</p>
              </button>
            ))}
          </div>
          <Button type="button" disabled={!selectedTemplateId || fromTemplate.isPending} onClick={createFromSelected}>
            {fromTemplate.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
            {fromTemplate.isPending ? "Creando..." : "Crear y editar"}
          </Button>
        </div>
      ) : null}
    </div>
  )
}
