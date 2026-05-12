import { useFormContext } from "react-hook-form"
import { purposeLabel } from "@/lib/labels"
import type { BlockPurpose } from "@/types/training"
import { cn } from "@/lib/utils"

const purposes: BlockPurpose[] = ["WARMUP", "ACTIVATION", "MAIN_LIFT", "ACCESSORY", "CONDITIONING", "CORE", "COOLDOWN", "OTHER"]

export function BlockPurposeSelector({ name, disabled }: { name: string; disabled?: boolean }) {
  const { setValue, watch } = useFormContext()
  const selected = watch(name) as BlockPurpose | null
  return (
    <div className="space-y-2">
      <p className="text-sm font-medium">Proposito</p>
      <div className="flex flex-wrap gap-2">
        {purposes.map((purpose) => (
          <button key={purpose} type="button" disabled={disabled} onClick={() => setValue(name, selected === purpose ? null : purpose, { shouldDirty: true })} className={cn("min-h-11 rounded-full border px-3 text-sm", selected === purpose ? "border-primary bg-primary text-primary-foreground" : "bg-white hover:bg-muted")}>{purposeLabel(purpose)}</button>
        ))}
      </div>
    </div>
  )
}