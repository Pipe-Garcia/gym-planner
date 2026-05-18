import { Flame, Plus, Target, Wind } from "lucide-react"
import { useFieldArray, useFormContext, useWatch } from "react-hook-form"
import { BlockEditor } from "@/components/template/BlockEditor"
import { emptyBlock } from "@/components/template/formDefaults"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { sectionIconStyleByGroup, sectionStyleByGroup, type SectionGroup } from "@/lib/sectionStyles"
import type { BlockPurpose, DayInput } from "@/types/training"

const WARMUP_PURPOSES = ["WARMUP", "ACTIVATION"] as const
const MAIN_PURPOSES = ["MAIN_LIFT", "ACCESSORY", "CONDITIONING", "CORE", "OTHER"] as const
const COOLDOWN_PURPOSES = ["COOLDOWN"] as const

type SectionKey = SectionGroup
type SectionItem = { idx: number; block: DayInput["blocks"][number]; fieldId: string }

function sectionOf(purpose: BlockPurpose | null): SectionKey | null {
  if (!purpose) return null
  if ((WARMUP_PURPOSES as readonly string[]).includes(purpose)) return "warmup"
  if ((MAIN_PURPOSES as readonly string[]).includes(purpose)) return "main"
  if ((COOLDOWN_PURPOSES as readonly string[]).includes(purpose)) return "cooldown"
  return null
}

export function TrainingSectionsEditor({
  dayIndex,
  context = "template",
  disabled = false,
  studentId,
  excludeRoutineId,
}: {
  dayIndex: number
  context?: "template" | "routine"
  disabled?: boolean
  studentId?: number
  excludeRoutineId?: number | null
}) {
  const { control } = useFormContext()
  const blocksPath = `days.${dayIndex}.blocks`
  const blocks = useFieldArray({ control, name: blocksPath, keyName: "_fieldId" })
  const watched = (useWatch({ control, name: blocksPath }) ?? []) as DayInput["blocks"]

  // Combinamos los valores observados con los ids estables del useFieldArray.
  // Usamos blocks.fields como fuente de verdad: si watched tiene desfase
  // momentáneo, blocks.fields siempre refleja la estructura actual.
  const indexed: SectionItem[] = blocks.fields.map((field, idx) => ({
    idx,
    block: (watched[idx] ?? {}) as DayInput["blocks"][number],
    fieldId: (field as unknown as { _fieldId: string })._fieldId,
  }))

  const warmup = indexed.filter(({ block }) => sectionOf(block.purpose) === "warmup")
  const main = indexed.filter(({ block }) => sectionOf(block.purpose) === "main")
  const cooldown = indexed.filter(({ block }) => sectionOf(block.purpose) === "cooldown")

  function addBlock(defaultPurpose: BlockPurpose) {
    blocks.append({
      ...emptyBlock(blocks.fields.length + 1),
      title: "",
      purpose: defaultPurpose,
    })
  }

  function moveUpInSection(globalIdx: number, sectionItems: SectionItem[]) {
    const position = sectionItems.findIndex((item) => item.idx === globalIdx)
    if (position > 0) blocks.swap(globalIdx, sectionItems[position - 1].idx)
  }

  function moveDownInSection(globalIdx: number, sectionItems: SectionItem[]) {
    const position = sectionItems.findIndex((item) => item.idx === globalIdx)
    if (position >= 0 && position < sectionItems.length - 1) {
      blocks.swap(globalIdx, sectionItems[position + 1].idx)
    }
  }

  return (
    <div className="space-y-4">
      <SectionPanel
        group="warmup"
        icon="flame"
        title="Calentamiento"
        emptyHint="Sin bloques de calentamiento. Agregá al menos uno."
        sectionItems={warmup}
        totalBlocks={blocks.fields.length}
        onAdd={() => addBlock("ACTIVATION")}
        addLabel="Agregar bloque de calentamiento"
        moveUp={moveUpInSection}
        moveDown={moveDownInSection}
        removeAt={blocks.remove}
        disabled={disabled}
        context={context}
        studentId={studentId}
        excludeRoutineId={excludeRoutineId}
        blocksPath={blocksPath}
      />
      <SectionPanel
        group="main"
        icon="target"
        title="Parte principal"
        emptyHint="Sin bloques en la parte principal. Agregá al menos uno."
        sectionItems={main}
        totalBlocks={blocks.fields.length}
        onAdd={() => addBlock("MAIN_LIFT")}
        addLabel="Agregar bloque a parte principal"
        moveUp={moveUpInSection}
        moveDown={moveDownInSection}
        removeAt={blocks.remove}
        disabled={disabled}
        context={context}
        studentId={studentId}
        excludeRoutineId={excludeRoutineId}
        blocksPath={blocksPath}
      />
      <SectionPanel
        group="cooldown"
        icon="wind"
        title="Vuelta a la calma"
        emptyHint="Sin bloques de vuelta a la calma. Agregá al menos uno."
        sectionItems={cooldown}
        totalBlocks={blocks.fields.length}
        onAdd={() => addBlock("COOLDOWN")}
        addLabel="Agregar bloque de vuelta a la calma"
        moveUp={moveUpInSection}
        moveDown={moveDownInSection}
        removeAt={blocks.remove}
        disabled={disabled}
        context={context}
        studentId={studentId}
        excludeRoutineId={excludeRoutineId}
        blocksPath={blocksPath}
      />
    </div>
  )
}

function SectionPanel({
  icon,
  group,
  title,
  emptyHint,
  sectionItems,
  totalBlocks,
  onAdd,
  addLabel,
  moveUp,
  moveDown,
  removeAt,
  blocksPath,
  context,
  disabled,
  studentId,
  excludeRoutineId,
}: {
  icon: "flame" | "target" | "wind"
  group: SectionGroup
  title: string
  emptyHint: string
  sectionItems: SectionItem[]
  totalBlocks: number
  onAdd: () => void
  addLabel: string
  moveUp: (globalIdx: number, sectionItems: SectionItem[]) => void
  moveDown: (globalIdx: number, sectionItems: SectionItem[]) => void
  removeAt: (globalIdx: number) => void
  blocksPath: string
  context: "template" | "routine"
  disabled: boolean
  studentId?: number
  excludeRoutineId?: number | null
}) {
  const Icon = icon === "flame" ? Flame : icon === "target" ? Target : Wind
  return (
    <section className={cn("rounded-md border p-4", sectionStyleByGroup(group))}>
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <span
            className={cn(
              "flex h-10 w-10 items-center justify-center rounded-md",
              sectionIconStyleByGroup(group)
            )}
          >
            <Icon className="h-5 w-5" />
          </span>
          <h2 className="font-semibold">{title}</h2>
        </div>
        <Button type="button" variant="outline" disabled={disabled} onClick={onAdd}>
          <Plus className="h-4 w-4" />
          {addLabel}
        </Button>
      </div>
      <div className="space-y-4">
        {sectionItems.length === 0 ? (
          <div className="rounded-md border border-dashed p-4 text-sm text-muted-foreground">
            {emptyHint}
          </div>
        ) : null}
        {sectionItems.map((item, position) => (
          <BlockEditor
            key={item.fieldId}
            blockIndex={item.idx}
            blockPath={`${blocksPath}.${item.idx}`}
            blocksLength={totalBlocks}
            disableUp={position === 0}
            disableDown={position === sectionItems.length - 1}
            onRemove={() => removeAt(item.idx)}
            onMoveUp={() => moveUp(item.idx, sectionItems)}
            onMoveDown={() => moveDown(item.idx, sectionItems)}
            disabled={disabled}
            context={context}
            studentId={studentId}
            excludeRoutineId={excludeRoutineId}
          />
        ))}
      </div>
    </section>
  )
}
