import { Info } from "lucide-react"
import { useMemo } from "react"
import type React from "react"
import { Controller, type FieldPath, type UseFormReturn } from "react-hook-form"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"

export interface CycleFormFields {
  newRoutineName: string
  newAssignedDate: string
  newStatus: "DRAFT" | "ACTIVE"
  copyGeneralNotes: boolean
  copyInternalNotes: boolean
  applyWeightAdjustment: boolean
  weightPercentage: number
  roundingStepKg: number | null
}

interface SharedFieldsProps<T extends CycleFormFields> {
  form: UseFormReturn<T>
  disabled?: boolean
}

export function NewRoutineFields<T extends CycleFormFields>({ form, disabled }: SharedFieldsProps<T>) {
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      <label className="space-y-1 text-sm font-medium">
        Nombre
        <Input disabled={disabled} {...form.register("newRoutineName" as FieldPath<T>)} />
        {form.formState.errors.newRoutineName ? <p className="text-xs text-destructive">{String(form.formState.errors.newRoutineName.message)}</p> : null}
      </label>
      <label className="space-y-1 text-sm font-medium">
        Fecha de inicio
        <Input type="date" disabled={disabled} {...form.register("newAssignedDate" as FieldPath<T>)} />
        {form.formState.errors.newAssignedDate ? <p className="text-xs text-destructive">{String(form.formState.errors.newAssignedDate.message)}</p> : null}
      </label>
      <label className="space-y-1 text-sm font-medium">
        Estado inicial
        <select
          className="flex h-11 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
          disabled={disabled}
          {...form.register("newStatus" as FieldPath<T>)}
        >
          <option value="DRAFT">Borrador (revisar antes de activar)</option>
          <option value="ACTIVE">Activa inmediatamente</option>
        </select>
      </label>
      <div className="space-y-3">
        <SwitchField
          label="Copiar notas visibles"
          description="Las notas generales de la rutina se copian."
          control={<Controller control={form.control} name={"copyGeneralNotes" as FieldPath<T>} render={({ field }) => <Switch checked={Boolean(field.value)} onCheckedChange={field.onChange} disabled={disabled} />} />}
        />
        <SwitchField
          label="Copiar notas internas"
          description="Las notas privadas del profesor se copian."
          control={<Controller control={form.control} name={"copyInternalNotes" as FieldPath<T>} render={({ field }) => <Switch checked={Boolean(field.value)} onCheckedChange={field.onChange} disabled={disabled} />} />}
        />
      </div>
    </div>
  )
}

export function WeightAdjustmentFields<T extends CycleFormFields>({ form, disabled }: SharedFieldsProps<T>) {
  const applyWeightAdjustment = form.watch("applyWeightAdjustment" as FieldPath<T>) as boolean
  const weightPercentage = form.watch("weightPercentage" as FieldPath<T>) as number
  const roundingStepKg = form.watch("roundingStepKg" as FieldPath<T>) as number | null
  const example = useMemo(() => weightExample(80, weightPercentage, roundingStepKg), [weightPercentage, roundingStepKg])

  return (
    <section className="space-y-3 rounded-md border bg-muted/20 p-4">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h3 className="text-sm font-semibold">Ajuste de pesos</h3>
          <p className="text-xs text-muted-foreground">Se aplica sobre la rutina nueva, sin tocar el historial.</p>
        </div>
        <div className="grid grid-cols-2 gap-2 rounded-md border bg-white p-1 text-sm">
          <button
            type="button"
            className={applyWeightAdjustment ? "rounded px-3 py-2 text-muted-foreground" : "rounded bg-muted px-3 py-2 font-medium"}
            disabled={disabled}
            onClick={() => form.setValue("applyWeightAdjustment" as FieldPath<T>, false as never)}
          >
            Sin ajuste
          </button>
          <button
            type="button"
            className={applyWeightAdjustment ? "rounded bg-primary px-3 py-2 font-medium text-primary-foreground" : "rounded px-3 py-2 text-muted-foreground"}
            disabled={disabled}
            onClick={() => form.setValue("applyWeightAdjustment" as FieldPath<T>, true as never)}
          >
            Aplicar ajuste
          </button>
        </div>
      </div>

      {applyWeightAdjustment ? (
        <div className="space-y-4">
          <label className="space-y-2 text-sm font-medium">
            Porcentaje <span className="font-semibold text-primary">{formatPercent(weightPercentage)}</span>
            <Controller
              control={form.control}
              name={"weightPercentage" as FieldPath<T>}
              render={({ field }) => (
                <input
                  type="range"
                  min={-30}
                  max={50}
                  step={1}
                  value={Number(field.value)}
                  disabled={disabled}
                  className="w-full accent-primary"
                  onChange={(event) => field.onChange(Number(event.target.value))}
                />
              )}
            />
          </label>
          <div className="flex justify-between text-xs text-muted-foreground"><span>-30%</span><span>+50%</span></div>

          <div className="space-y-2">
            <Label>Redondear a</Label>
            <Controller
              control={form.control}
              name={"roundingStepKg" as FieldPath<T>}
              render={({ field }) => (
                <div className="grid gap-2 sm:grid-cols-2">
                  {roundingOptions.map((option) => (
                    <label key={option.label} className="flex items-center gap-2 rounded-md border bg-white px-3 py-2 text-sm">
                      <input
                        type="radio"
                        name="roundingStepKg"
                        checked={field.value === option.value}
                        disabled={disabled}
                        onChange={() => field.onChange(option.value)}
                      />
                      {option.label}
                    </label>
                  ))}
                </div>
              )}
            />
          </div>

          <div className="rounded-md border bg-white p-3 text-sm">
            <p className="font-medium">Ejemplo</p>
            <p className="mt-1 text-muted-foreground">Un ejercicio con peso actual de 80 kg</p>
            <p className="mt-2">→ {formatPercent(weightPercentage)} = {formatWeight(example.raw)} kg</p>
            <p>→ {roundingStepKg ? `redondeado a ${roundingStepKg} kg = ${formatWeight(example.rounded)} kg` : `sin redondeo = ${formatWeight(example.rounded)} kg`}</p>
          </div>

          <p className="flex gap-2 text-xs text-muted-foreground"><Info className="h-4 w-4 shrink-0" />Los ejercicios sin peso (solo tiempo, distancia o reps) no se modifican.</p>
          <p className="flex gap-2 text-xs text-muted-foreground"><Info className="h-4 w-4 shrink-0" />Si aplicás el ajuste y después volvés a aplicar otro, el segundo se calcula sobre los pesos ya actualizados.</p>
        </div>
      ) : null}
    </section>
  )
}

export const roundingOptions = [
  { label: "Sin redondeo", value: null },
  { label: "0.5 kg", value: 0.5 },
  { label: "1 kg", value: 1 },
  { label: "2.5 kg — habitual en discos de gimnasio", value: 2.5 },
  { label: "5 kg", value: 5 },
]

export function suggestNextCycleName(name: string) {
  const match = name.match(/^(.*) — Ciclo (\d+)$/)
  if (!match) return `${name} — Ciclo 2`
  return `${match[1]} — Ciclo ${Number(match[2]) + 1}`
}

export function todayInputValue() {
  return new Date().toISOString().slice(0, 10)
}

export function weightExample(weight: number, percentage: number, roundingStepKg: number | null) {
  const raw = weight * (1 + percentage / 100)
  return { raw, rounded: roundNearest(raw, roundingStepKg) }
}

function roundNearest(value: number, step: number | null) {
  if (!step) return Number(value.toFixed(2))
  return Number((Math.floor(value / step + 0.5) * step).toFixed(2))
}

function SwitchField({ label, description, control }: { label: string; description: string; control: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-3">
      <div>
        <p className="text-sm font-medium">{label}</p>
        <p className="text-xs text-muted-foreground">{description}</p>
      </div>
      {control}
    </div>
  )
}

function formatPercent(value: number) {
  return value > 0 ? `+${value}%` : `${value}%`
}

function formatWeight(value: number) {
  return Number.isInteger(value) ? value.toFixed(0) : value.toFixed(2).replace(/0$/, "")
}
