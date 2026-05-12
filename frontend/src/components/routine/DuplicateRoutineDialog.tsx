import { useEffect, useMemo, useState } from "react"
import { useNavigate } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { useDuplicateRoutine } from "@/hooks/useRoutines"
import { useStudents } from "@/hooks/useStudents"
import { useToast } from "@/hooks/useToast"
import { fromDateInputValue, toDateInputValue } from "@/lib/date"
import type { RoutineStatus } from "@/types/training"

interface DuplicateRoutineDialogProps {
  routine: { id: number; name: string } | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function DuplicateRoutineDialog({ routine, open, onOpenChange }: DuplicateRoutineDialogProps) {
  const navigate = useNavigate()
  const toast = useToast()
  const duplicate = useDuplicateRoutine()
  const students = useStudents({ active: true, page: 0, size: 100, sort: "lastName,asc" })
  const [targetStudentId, setTargetStudentId] = useState("")
  const [name, setName] = useState("")
  const [assignedDate, setAssignedDate] = useState(todayInputValue())
  const [status, setStatus] = useState<Extract<RoutineStatus, "DRAFT" | "ACTIVE">>("DRAFT")
  const studentOptions = useMemo(() => students.data?.content ?? [], [students.data?.content])

  useEffect(() => {
    if (!open || !routine) return
    setName(`${routine.name} (copia)`)
    setAssignedDate(todayInputValue())
    setStatus("DRAFT")
    setTargetStudentId("")
  }, [open, routine])

  async function submit() {
    if (!routine || !targetStudentId) {
      toast.error("Selecciona un alumno destino.")
      return
    }
    const copy = await duplicate.mutateAsync({
      id: routine.id,
      data: {
        targetStudentId: Number(targetStudentId),
        name: name.trim() || null,
        assignedDate: fromDateInputValue(assignedDate),
        status,
      },
    })
    toast.success("Rutina duplicada.")
    onOpenChange(false)
    navigate(`/students/${copy.studentId}/routines/${copy.id}`)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Duplicar rutina</DialogTitle>
          <DialogDescription>Copiara dias, bloques, ejercicios, sets, pesos y parametros a otro alumno.</DialogDescription>
        </DialogHeader>
        <div className="grid gap-4">
          <label className="space-y-1 text-sm font-medium">
            Alumno destino
            <select value={targetStudentId} onChange={(event) => setTargetStudentId(event.target.value)} className="min-h-11 w-full rounded-md border bg-white px-3 text-sm">
              <option value="">Seleccionar alumno</option>
              {studentOptions.map((student) => (
                <option key={student.id} value={student.id}>
                  {student.lastName}, {student.firstName}
                </option>
              ))}
            </select>
          </label>
          <label className="space-y-1 text-sm font-medium">
            Nombre nueva rutina
            <Input value={name} onChange={(event) => setName(event.target.value)} />
          </label>
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="space-y-1 text-sm font-medium">
              Fecha asignada
              <Input type="date" value={toDateInputValue(assignedDate)} onChange={(event) => setAssignedDate(event.target.value)} />
            </label>
            <label className="space-y-1 text-sm font-medium">
              Estado
              <select value={status} onChange={(event) => setStatus(event.target.value as Extract<RoutineStatus, "DRAFT" | "ACTIVE">)} className="min-h-11 w-full rounded-md border bg-white px-3 text-sm">
                <option value="DRAFT">Borrador</option>
                <option value="ACTIVE">Activa</option>
              </select>
            </label>
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancelar</Button>
            <Button type="button" disabled={duplicate.isPending} onClick={submit}>Duplicar</Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

function todayInputValue() {
  const date = new Date()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${date.getFullYear()}-${month}-${day}`
}
