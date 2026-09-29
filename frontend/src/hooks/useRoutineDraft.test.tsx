import { act, fireEvent, render, screen } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { useState } from "react"
import { useForm } from "react-hook-form"
import { useRoutineDraft } from "./useRoutineDraft"
import { defaultDay } from "@/components/template/formDefaults"
import { loadDraft } from "@/lib/routine-draft"
import type { RoutineFormValues } from "@/schemas/template.schema"

const scope = { gymId: 1, userId: 2, routineId: 3 }
function Fixture() {
  const form = useForm<RoutineFormValues>({ defaultValues: {
    name: "Servidor", description: null, sport: null, objective: null, level: null,
    generalNotes: null, internalNotes: null, assignedDate: "2026-09-25", status: "DRAFT", days: [defaultDay(1)],
  } })
  const draft = useRoutineDraft(form, scope, 0, true)
  return <><label>Nombre<input {...form.register("name")} /></label><output>{draft.status}</output><button onClick={draft.clearOnSuccess}>Éxito</button></>
}

beforeEach(() => { localStorage.clear(); vi.useFakeTimers() })
afterEach(() => vi.useRealTimers())

describe("useRoutineDraft lifecycle", () => {
  it("writes dirty values after debounce and reports protected only after success", () => {
    render(<Fixture />)
    fireEvent.change(screen.getByRole("textbox", { name: "Nombre" }), { target: { value: "Local" } })
    expect(screen.getByRole("status")).toHaveTextContent("unsaved")
    expect(loadDraft(scope)).toBeNull()
    act(() => vi.advanceTimersByTime(600))
    expect(loadDraft(scope)?.formValues.name).toBe("Local")
    expect(screen.getByRole("status")).toHaveTextContent("protected")
  })

  it("flushes on pagehide before debounce and does not resurrect after save success", () => {
    render(<Fixture />)
    fireEvent.change(screen.getByRole("textbox", { name: "Nombre" }), { target: { value: "Local" } })
    act(() => window.dispatchEvent(new Event("pagehide")))
    expect(loadDraft(scope)?.formValues.name).toBe("Local")
    fireEvent.change(screen.getByRole("textbox", { name: "Nombre" }), { target: { value: "Más local" } })
    fireEvent.click(screen.getByRole("button", { name: "Éxito" }))
    act(() => vi.advanceTimersByTime(1000))
    expect(loadDraft(scope)).toBeNull()
  })

  it("flushes when the page becomes hidden", () => {
    render(<Fixture />)
    fireEvent.change(screen.getByRole("textbox", { name: "Nombre" }), { target: { value: "Oculto" } })
    const previous = Object.getOwnPropertyDescriptor(document, "visibilityState")
    Object.defineProperty(document, "visibilityState", { configurable: true, value: "hidden" })
    act(() => document.dispatchEvent(new Event("visibilitychange")))
    expect(loadDraft(scope)?.formValues.name).toBe("Oculto")
    if (previous) Object.defineProperty(document, "visibilityState", previous)
    else Reflect.deleteProperty(document, "visibilityState")
  })

  it("removes an obsolete draft when the form returns from dirty to clean", () => {
    const ui = render(<Fixture />)
    const input = screen.getByRole("textbox", { name: "Nombre" })
    fireEvent.change(input, { target: { value: "Local" } })
    act(() => vi.advanceTimersByTime(600))
    expect(loadDraft(scope)?.formValues.name).toBe("Local")
    fireEvent.change(input, { target: { value: "Servidor" } })
    expect(loadDraft(scope)).toBeNull()
    act(() => vi.advanceTimersByTime(1000))
    ui.unmount()
    expect(loadDraft(scope)).toBeNull()
  })

  it("updates selectedDay without another RHF field change once the form is dirty", () => {
    function SelectedDayFixture() {
      const [selectedDay, setSelectedDay] = useState(0)
      const form = useForm<RoutineFormValues>({ defaultValues: {
        name: "Servidor", description: null, sport: null, objective: null, level: null,
        generalNotes: null, internalNotes: null, assignedDate: "2026-09-25", status: "DRAFT", days: [defaultDay(1)],
      } })
      useRoutineDraft(form, scope, selectedDay, true)
      return <><label>Nombre<input {...form.register("name")} /></label><button onClick={() => setSelectedDay(3)}>Día 4</button></>
    }
    render(<SelectedDayFixture />)
    fireEvent.change(screen.getByRole("textbox", { name: "Nombre" }), { target: { value: "Local" } })
    act(() => vi.advanceTimersByTime(600))
    expect(loadDraft(scope)?.selectedDay).toBe(0)
    fireEvent.click(screen.getByRole("button", { name: "Día 4" }))
    act(() => vi.advanceTimersByTime(600))
    expect(loadDraft(scope)?.selectedDay).toBe(3)
  })
})
