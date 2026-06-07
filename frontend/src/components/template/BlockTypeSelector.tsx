import { Info } from "lucide-react"
import { useFormContext, useWatch } from "react-hook-form"
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip"
import { structuralTypeLabel } from "@/lib/labels"
import type { EditableBlockStructuralType } from "@/types/training"

const descriptions: Record<EditableBlockStructuralType, string> = {
  STANDARD: "Series tradicionales con repeticiones y peso fijos. Ej: 3x10 con 20 kg.",
  CIRCUIT: "Varios ejercicios rotando durante un tiempo total. Ej: 3 ejercicios rotando 12 minutos.",
  GROUPED_SET: "Varios ejercicios ejecutados como una unidad por vueltas. Ej: biserie o triserie.",
  PYRAMID: "Las series escalan ascendente. Ej: 6 reps con 60kg -> 8 reps con 50kg -> 10 reps con 40kg.",
  REVERSE_PYRAMID: "Las series escalan descendente. Empieza fuerte y baja peso. Ej: 6 reps con 80kg -> 8 reps con 70kg -> 10 reps con 60kg.",
  DROP_SET: "Una serie principal al fallo, seguida de bajadas inmediatas con menos peso.",
  REST_PAUSE: "Una serie principal con micro-descansos cortos para extender el trabajo.",
  CLUSTER: "Una serie fragmentada en mini-bloques con descanso corto entre ellos. Ej: 3+3+3 con 10s entre cada 3.",
}

const options = Object.keys(descriptions) as EditableBlockStructuralType[]

export function BlockTypeSelector({ name, disabled }: { name: string; disabled?: boolean }) {
  const { register, control } = useFormContext()
  const value = useWatch({ control, name }) as EditableBlockStructuralType | undefined
  const selected = value ?? "STANDARD"

  return (
    <div className="flex items-stretch gap-2">
      <select
        disabled={disabled}
        className="min-h-11 flex-1 rounded-md border bg-white px-3 text-sm"
        {...register(name)}
      >
        {options.map((option) => (
          <option key={option} value={option}>
            {structuralTypeLabel(option)}
          </option>
        ))}
      </select>
      <TooltipProvider delayDuration={200}>
        <Tooltip>
          <TooltipTrigger asChild>
            <button
              type="button"
              className="flex min-h-11 w-11 shrink-0 items-center justify-center rounded-md border bg-white text-muted-foreground hover:text-foreground"
              aria-label="Ver descripcion del tipo de bloque"
            >
              <Info className="h-4 w-4" />
            </button>
          </TooltipTrigger>
          <TooltipContent
            side="left"
            className="w-[280px] max-w-[calc(100vw-2rem)] whitespace-normal text-sm leading-relaxed"
          >
            <p>{descriptions[selected]}</p>
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
    </div>
  )
}
