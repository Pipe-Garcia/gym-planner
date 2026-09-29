import { act, fireEvent, render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { MemoryRouter, Route, Routes, Link } from "react-router-dom"
import { beforeEach, describe, expect, it, vi } from "vitest"
import type { Routine } from "@/types/training"
import { defaultDay } from "@/components/template/formDefaults"
import { draftKey, loadDraft, saveDraft } from "@/lib/routine-draft"
import type { RoutineFormValues } from "@/schemas/template.schema"
import { RoutineEditorPage } from "./RoutineEditorPage"

const mocks = vi.hoisted(() => ({
  routines: {} as Record<number, unknown>,
  pending: false,
  mutateAsync: vi.fn(),
  toast: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
}))

vi.mock("@/hooks/useRoutines", () => ({
  useRoutine: (id: number) => ({ data: mocks.routines[id], refetch: vi.fn() }),
  useUpdateRoutine: () => ({ isPending: mocks.pending, mutateAsync: mocks.mutateAsync }),
}))
vi.mock("@/hooks/useAuth", () => ({ useAuth: () => ({ user: { id: 2, gymId: 1 } }) }))
vi.mock("@/hooks/useToast", () => ({ useToast: () => mocks.toast }))
vi.mock("@/components/routine/RoutineActionsBar", () => ({ RoutineActionsBar: ({ disabled }: { disabled: boolean }) => <output data-testid="actions">{disabled ? "blocked" : "available"}</output> }))
vi.mock("@/components/routine/RoutineIdentityHeader", () => ({ RoutineIdentityHeader: () => <div>Rutina</div> }))
vi.mock("@/components/template/TemplateMetadataForm", async () => {
  const { useFormContext } = await import("react-hook-form")
  return { TemplateMetadataForm: ({ readOnly }: { readOnly: boolean }) => {
    const form = useFormContext()
    return <><label>Nombre<input {...form.register("name")} disabled={readOnly} /></label><output data-testid="dirty">{form.formState.isDirty ? "dirty" : "clean"}</output></>
  } }
})
vi.mock("@/components/template/TrainingDaysEditor", () => ({ TrainingDaysEditor: ({ disabled, selectedDay, onSelectedDayChange }: { disabled: boolean; selectedDay: number; onSelectedDayChange: (day: number) => void }) => <><output data-testid="selected-day">{selectedDay}</output><button type="button" disabled={disabled} onClick={() => onSelectedDayChange(1)}>Día siguiente</button></> }))

const scope = { gymId: 1, userId: 2, routineId: 3 }
function routine(id = 3, name = "Servidor"): Routine {
  return {
    id, studentId: 4, studentName: "Alumno", name, objective: null, sourceTemplateId: null, sourceTemplateName: null,
    status: "DRAFT", assignedDate: "2026-09-25", finishedDate: null, finishedAt: null, closureNotes: null,
    previousRoutineId: null, dayCount: 2, blockCount: 0, exerciseCount: 0, createdAt: "2026-09-25T12:00:00Z",
    updatedAt: "2026-09-25T12:00:00Z", generalNotes: null, internalNotes: "Interno", createdByUserId: 2,
    days: [defaultDay(1), defaultDay(2)].map((day, index) => ({ ...day, id: index + 10, blocks: [] })),
  }
}
function formValues(name = "Servidor"): RoutineFormValues {
  return {
    studentId: 4, name, description: null, sport: null, objective: null, level: null,
    assignedDate: "2026-09-25", finishedDate: null, generalNotes: null, internalNotes: "Interno", status: "DRAFT",
    days: [defaultDay(1), defaultDay(2)],
  }
}
function App() {
  return <MemoryRouter initialEntries={["/students/4/routines/3/edit"]}>
    <Link to="/students/4/routines/4/edit">Otra rutina</Link>
    <Routes><Route path="/students/:studentId/routines/:routineId/edit" element={<RoutineEditorPage />} /><Route path="/students/:studentId/routines/:routineId" element={<div>Vista</div>} /></Routes>
  </MemoryRouter>
}

beforeEach(() => {
  localStorage.clear()
  mocks.routines = { 3: routine(), 4: routine(4, "Otra") }
  mocks.pending = false
  mocks.mutateAsync.mockReset().mockResolvedValue({})
  mocks.toast.success.mockReset()
  mocks.toast.error.mockReset()
})

describe("RoutineEditorPage resilience", () => {
  it("hydrates from server and lets a clean refetch update the form", async () => {
    const ui = render(<App />)
    expect(await screen.findByRole("textbox", { name: "Nombre" })).toHaveValue("Servidor")
    mocks.routines[3] = routine(3, "Servidor actualizado")
    ui.rerender(<App />)
    await waitFor(() => expect(screen.getByRole("textbox", { name: "Nombre" })).toHaveValue("Servidor actualizado"))
  })

  it("does not overwrite dirty edits on refetch", async () => {
    const ui = render(<App />)
    const input = await screen.findByRole("textbox", { name: "Nombre" })
    fireEvent.change(input, { target: { value: "Local" } })
    expect(input).toHaveValue("Local")
    mocks.routines[3] = routine(3, "Servidor actualizado")
    ui.rerender(<App />)
    expect(screen.getByRole("textbox", { name: "Nombre" })).toHaveValue("Local")
  })

  it("offers a blocking recovery and restores values, selected day and dirty state", async () => {
    expect(saveDraft(scope, formValues("Borrador"), 1)).toBe(true)
    render(<App />)
    expect(await screen.findByRole("dialog")).toHaveTextContent("Recuperar cambios")
    expect(screen.getByRole("textbox", { name: "Nombre", hidden: true })).toBeDisabled()
    await userEvent.setup().click(screen.getByRole("button", { name: "Recuperar" }))
    expect(screen.getByRole("textbox", { name: "Nombre" })).toHaveValue("Borrador")
    expect(screen.getByTestId("selected-day")).toHaveTextContent("1")
    expect(screen.getByTestId("dirty")).toHaveTextContent("dirty")
    const ui = screen.getByRole("textbox", { name: "Nombre" })
    fireEvent.change(ui, { target: { value: "Todavía local" } })
  })

  it("discards draft and keeps server values", async () => {
    saveDraft(scope, formValues("Borrador"), 1)
    render(<App />)
    await userEvent.setup().click(await screen.findByRole("button", { name: "Descartar" }))
    expect(screen.getByRole("textbox", { name: "Nombre" })).toHaveValue("Servidor")
    expect(loadDraft(scope)).toBeNull()
  })

  it("skips the modal when draft content equals server", async () => {
    saveDraft(scope, formValues(), 99)
    render(<App />)
    expect(await screen.findByRole("textbox", { name: "Nombre" })).toHaveValue("Servidor")
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    expect(loadDraft(scope)).toBeNull()
  })

  it("clamps a recovered day and keeps recovered changes across refetch", async () => {
    saveDraft(scope, formValues("Borrador"), 99)
    const ui = render(<App />)
    await userEvent.setup().click(await screen.findByRole("button", { name: "Recuperar" }))
    expect(screen.getByTestId("selected-day")).toHaveTextContent("1")
    mocks.routines[3] = routine(3, "Servidor nuevo")
    ui.rerender(<App />)
    expect(screen.getByRole("textbox", { name: "Nombre" })).toHaveValue("Borrador")
  })

  it("does not leak the previous form when route ID changes", async () => {
    render(<App />)
    fireEvent.change(await screen.findByRole("textbox", { name: "Nombre" }), { target: { value: "Local" } })
    await userEvent.setup().click(screen.getByRole("link", { name: "Otra rutina" }))
    expect(await screen.findByRole("textbox", { name: "Nombre" })).toHaveValue("Otra")
  })

  it("blocks mutating controls while PUT is pending", async () => {
    const ui = render(<App />)
    await screen.findByRole("textbox", { name: "Nombre" })
    mocks.pending = true
    ui.rerender(<App />)
    expect(screen.getByRole("textbox", { name: "Nombre" })).toBeDisabled()
    expect(screen.getByRole("button", { name: "Día siguiente" })).toBeDisabled()
    expect(screen.getByTestId("actions")).toHaveTextContent("blocked")
  })

  it("confirms draft protection during a prolonged save only after a successful local snapshot", () => {
    vi.useFakeTimers()
    try {
      const ui = render(<App />)
      fireEvent.change(screen.getByRole("textbox", { name: "Nombre" }), { target: { value: "Local" } })
      act(() => window.dispatchEvent(new Event("pagehide")))
      expect(screen.getByText(/borrador protegido/)).toBeInTheDocument()
      mocks.pending = true
      ui.rerender(<App />)
      act(() => vi.advanceTimersByTime(4000))
      expect(screen.getByText(/Tus cambios siguen protegidos/)).toBeInTheDocument()
    } finally {
      vi.useRealTimers()
    }
  })

  it("does not claim protection during a prolonged save when local storage failed", () => {
    vi.useFakeTimers()
    const storageFailure = vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => { throw Error("quota") })
    try {
      const ui = render(<App />)
      fireEvent.change(screen.getByRole("textbox", { name: "Nombre" }), { target: { value: "Local" } })
      act(() => window.dispatchEvent(new Event("pagehide")))
      expect(screen.getByText("Sin guardar")).toBeInTheDocument()
      mocks.pending = true
      ui.rerender(<App />)
      act(() => vi.advanceTimersByTime(4000))
      const status = screen.getByText(/El servidor está tardando/)
      expect(status).toHaveTextContent("Esperá a que finalice el guardado")
      expect(status).not.toHaveTextContent("protegidos")
    } finally {
      storageFailure.mockRestore()
      vi.useRealTimers()
    }
  })

  it("removes draft on success and preserves it on failed save", async () => {
    const expectedErrorLog = vi.spyOn(console, "error").mockImplementation(() => {})
    saveDraft(scope, formValues("Local"), 0)
    const ui = render(<App />)
    await userEvent.setup().click(await screen.findByRole("button", { name: "Descartar" }))
    saveDraft(scope, formValues("Local"), 0)
    fireEvent.change(screen.getByRole("textbox", { name: "Nombre" }), { target: { value: "Local" } })
    mocks.mutateAsync.mockRejectedValueOnce(Error("network"))
    await userEvent.setup().click(screen.getByRole("button", { name: "Guardar cambios" }))
    await waitFor(() => expect(mocks.toast.error).toHaveBeenCalled())
    expect(localStorage.getItem(draftKey(scope))).not.toBeNull()
    expect(screen.getByRole("textbox", { name: "Nombre" })).not.toBeDisabled()
    await userEvent.setup().click(screen.getByRole("button", { name: "Guardar cambios" }))
    await waitFor(() => expect(screen.getByText("Vista")).toBeInTheDocument())
    expect(localStorage.getItem(draftKey(scope))).toBeNull()
    ui.unmount()
    expectedErrorLog.mockRestore()
  })
})
