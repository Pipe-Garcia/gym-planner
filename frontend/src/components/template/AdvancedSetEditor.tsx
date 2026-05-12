import { Trash2 } from "lucide-react"
import { useFormContext } from "react-hook-form"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { emptySet } from "@/components/template/formDefaults"
import { setKindLabel } from "@/lib/labels"
import type { MeasurementType } from "@/types/exercise"
import type { ExerciseSetInput, SetKind } from "@/types/training"

const setKinds: SetKind[] = ["NORMAL", "WARMUP", "FAILURE", "DROP", "REST_PAUSE_PORTION"]

type Column = "reps" | "weight" | "time" | "distance" | "rest" | "rpe" | "failure" | "notes"
const nullableNumberRegister = {
  setValueAs: (value: unknown) => {
    if (value === "" || value === undefined || value === null) return null
    const next = Number(value)
    return Number.isNaN(next) ? null : next
  },
}

interface Props {
  name: string
  setsField: { fields: { id: string }[]; append: (item: ExerciseSetInput) => void; remove: (index: number) => void }
  measurement: MeasurementType
  context: "template" | "routine"
  disabled?: boolean
}

export function AdvancedSetEditor({ name, setsField, measurement, context, disabled }: Props) {
  const columns = visibleColumns(measurement, context)
  return (
    <div className="space-y-3">
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-muted-foreground">
              <th>#</th>
              <th>Tipo</th>
              {columns.includes("reps") ? <th>Reps</th> : null}
              {columns.includes("weight") ? <th>Peso</th> : null}
              {columns.includes("time") ? <th>Tiempo</th> : null}
              {columns.includes("distance") ? <th>Distancia</th> : null}
              {columns.includes("rest") ? <th>Descanso</th> : null}
              {columns.includes("rpe") ? <th>RPE</th> : null}
              {columns.includes("failure") ? <th>Fallo</th> : null}
              {columns.includes("notes") ? <th>Notas</th> : null}
              <th />
            </tr>
          </thead>
          <tbody>
            {setsField.fields.map((field, index) => (
              <SetRow key={field.id} name={name} index={index} columns={columns} remove={() => setsField.remove(index)} disabled={disabled} />
            ))}
          </tbody>
        </table>
      </div>
      <div className="grid gap-3 md:hidden">
        {setsField.fields.map((field, index) => (
          <SetCard key={field.id} name={name} index={index} columns={columns} remove={() => setsField.remove(index)} disabled={disabled} />
        ))}
      </div>
      <Button type="button" variant="outline" disabled={disabled} onClick={() => setsField.append(emptySet(setsField.fields.length + 1))}>
        + agregar set
      </Button>
    </div>
  )
}

function visibleColumns(measurement: MeasurementType, context: "template" | "routine"): Column[] {
  if (measurement === "TIME") return ["time", "rest", "notes"]
  if (measurement === "DISTANCE") return ["distance", "rest", "notes"]
  if (measurement === "CIRCUIT_REPS") return ["reps", "notes"]
  if (measurement === "REPS_ONLY") return ["reps", "rest", "rpe", "failure", "notes"]
  return context === "routine" ? ["reps", "weight", "rest", "rpe", "failure", "notes"] : ["reps", "rest", "rpe", "failure", "notes"]
}

function SetKindSelect({ name, index, disabled }: { name: string; index: number; disabled?: boolean }) {
  const { register } = useFormContext()
  return (
    <select disabled={disabled} defaultValue="NORMAL" className="h-9 rounded-md border bg-white px-2" {...register(`${name}.${index}.setKind`)}>
      {setKinds.map((kind) => <option key={kind} value={kind}>{setKindLabel(kind)}</option>)}
    </select>
  )
}

function SetRow({ name, index, columns, remove, disabled }: { name: string; index: number; columns: Column[]; remove: () => void; disabled?: boolean }) {
  const { register } = useFormContext()
  return (
    <tr className="border-t align-top">
      <td className="p-1">{index + 1}</td>
      <td className="p-1"><SetKindSelect name={name} index={index} disabled={disabled} /></td>
      {columns.includes("reps") ? <td className="p-1"><Input type="number" inputMode="numeric" disabled={disabled} className="no-spinner w-20" {...register(`${name}.${index}.targetReps`, nullableNumberRegister)} /></td> : null}
      {columns.includes("weight") ? <td className="p-1"><Input type="number" inputMode="decimal" step="0.5" disabled={disabled} className="no-spinner w-20" {...register(`${name}.${index}.targetWeightKg`, nullableNumberRegister)} /></td> : null}
      {columns.includes("time") ? <td className="p-1"><Input type="number" inputMode="numeric" disabled={disabled} className="no-spinner w-20" {...register(`${name}.${index}.targetTimeSeconds`, nullableNumberRegister)} /></td> : null}
      {columns.includes("distance") ? <td className="p-1"><Input type="number" inputMode="numeric" disabled={disabled} className="no-spinner w-24" {...register(`${name}.${index}.targetDistanceMeters`, nullableNumberRegister)} /></td> : null}
      {columns.includes("rest") ? <td className="p-1"><Input type="number" inputMode="numeric" disabled={disabled} className="no-spinner w-20" {...register(`${name}.${index}.restAfterSeconds`, nullableNumberRegister)} /></td> : null}
      {columns.includes("rpe") ? <td className="p-1"><Input type="number" inputMode="numeric" disabled={disabled} className="no-spinner w-16" {...register(`${name}.${index}.rpe`, nullableNumberRegister)} /></td> : null}
      {columns.includes("failure") ? <td className="p-1"><input type="checkbox" disabled={disabled} {...register(`${name}.${index}.toFailure`)} /></td> : null}
      {columns.includes("notes") ? <td className="p-1"><Input disabled={disabled} {...register(`${name}.${index}.notes`)} /></td> : null}
      <td className="p-1"><Button type="button" size="icon" variant="ghost" disabled={disabled} onClick={remove}><Trash2 className="h-4 w-4" /></Button></td>
    </tr>
  )
}

function SetCard({ name, index, columns, remove, disabled }: { name: string; index: number; columns: Column[]; remove: () => void; disabled?: boolean }) {
  const { register } = useFormContext()
  return (
    <div className="rounded-md border p-3">
      <div className="mb-2 flex items-center justify-between">
        <p className="font-medium">Set {index + 1}</p>
        <Button type="button" size="icon" variant="ghost" disabled={disabled} onClick={remove}><Trash2 className="h-4 w-4" /></Button>
      </div>
      <div className="grid gap-2">
        <SetKindSelect name={name} index={index} disabled={disabled} />
        {columns.includes("reps") ? <Input type="number" inputMode="numeric" className="no-spinner" placeholder="Reps" disabled={disabled} {...register(`${name}.${index}.targetReps`, nullableNumberRegister)} /> : null}
        {columns.includes("weight") ? <Input type="number" inputMode="decimal" className="no-spinner" step="0.5" placeholder="Peso" disabled={disabled} {...register(`${name}.${index}.targetWeightKg`, nullableNumberRegister)} /> : null}
        {columns.includes("time") ? <Input type="number" inputMode="numeric" className="no-spinner" placeholder="Tiempo" disabled={disabled} {...register(`${name}.${index}.targetTimeSeconds`, nullableNumberRegister)} /> : null}
        {columns.includes("distance") ? <Input type="number" inputMode="numeric" className="no-spinner" placeholder="Distancia" disabled={disabled} {...register(`${name}.${index}.targetDistanceMeters`, nullableNumberRegister)} /> : null}
        {columns.includes("rest") ? <Input type="number" inputMode="numeric" className="no-spinner" placeholder="Descanso" disabled={disabled} {...register(`${name}.${index}.restAfterSeconds`, nullableNumberRegister)} /> : null}
        {columns.includes("rpe") ? <Input type="number" inputMode="numeric" className="no-spinner" placeholder="RPE" disabled={disabled} {...register(`${name}.${index}.rpe`, nullableNumberRegister)} /> : null}
        {columns.includes("failure") ? <label className="flex min-h-11 items-center gap-2 text-sm"><input type="checkbox" disabled={disabled} {...register(`${name}.${index}.toFailure`)} />Al fallo</label> : null}
        {columns.includes("notes") ? <Input placeholder="Notas" disabled={disabled} {...register(`${name}.${index}.notes`)} /> : null}
      </div>
    </div>
  )
}
