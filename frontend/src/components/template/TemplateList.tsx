import { TemplateCard } from "@/components/template/TemplateCard"
import type { TemplateSummary } from "@/types/training"

interface Props { templates: TemplateSummary[]; onDuplicate: (id: number) => void; onDeactivate: (id: number) => void; onReactivate: (id: number) => void }

export function TemplateList({ templates, onDuplicate, onDeactivate, onReactivate }: Props) {
  return <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{templates.map((template) => <TemplateCard key={template.id} template={template} onDuplicate={() => onDuplicate(template.id)} onDeactivate={() => onDeactivate(template.id)} onReactivate={() => onReactivate(template.id)} />)}</div>
}