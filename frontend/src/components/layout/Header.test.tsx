import { fireEvent, render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { useForm } from "react-hook-form"
import { defaultDay } from "@/components/template/formDefaults"
import { useRoutineDraft } from "@/hooks/useRoutineDraft"
import { clearAuthStorage } from "@/lib/auth-storage"
import { loadDraft, saveDraft } from "@/lib/routine-draft"
import type { RoutineFormValues } from "@/schemas/template.schema"
import { Header } from "./Header"

const mocks = vi.hoisted(() => ({ logout: vi.fn() }))
vi.mock("@/hooks/useAuth", () => ({ useAuth: () => ({ user: { id: 2, gymId: 1, fullName: "Usuario" }, logout: mocks.logout }) }))
const scope = { gymId: 1, userId: 2, routineId: 3 }
const values = { name: "Local", description: null, sport: null, objective: null, level: null, generalNotes: null, internalNotes: null, assignedDate: "2026-09-25", status: "DRAFT" as const, days: [defaultDay(1)] }

function DirtyEditorWithHeader() {
  const form = useForm<RoutineFormValues>({ defaultValues: values })
  useRoutineDraft(form, scope, 0, true)
  return <><Header onMenuClick={() => {}} /><label>Nombre<input {...form.register("name")} /></label></>
}

beforeEach(() => { localStorage.clear(); mocks.logout.mockReset() })

describe("drafts and authentication", () => {
  it("manual logout removes only this user's drafts before logout", async () => {
    saveDraft(scope, values, 0)
    saveDraft({ ...scope, userId: 9 }, values, 0)
    mocks.logout.mockImplementation(() => expect(loadDraft(scope)).toBeNull())
    render(<Header onMenuClick={() => {}} />)
    await userEvent.setup().click(screen.getByRole("button", { name: /Salir/i }))
    expect(mocks.logout).toHaveBeenCalledOnce()
    expect(loadDraft({ ...scope, userId: 9 })).not.toBeNull()
  })

  it("manual logout cannot be undone by the dirty editor cleanup", async () => {
    const ui = render(<DirtyEditorWithHeader />)
    fireEvent.change(screen.getByRole("textbox", { name: "Nombre" }), { target: { value: "Pendiente" } })
    fireEvent(window, new Event("pagehide"))
    expect(loadDraft(scope)?.formValues.name).toBe("Pendiente")
    await userEvent.setup().click(screen.getByRole("button", { name: /Salir/i }))
    ui.unmount()
    expect(loadDraft(scope)).toBeNull()
  })

  it("forced auth storage clearing leaves drafts available after editor unmount", () => {
    const ui = render(<DirtyEditorWithHeader />)
    fireEvent.change(screen.getByRole("textbox", { name: "Nombre" }), { target: { value: "Pendiente" } })
    fireEvent(window, new Event("pagehide"))
    clearAuthStorage()
    ui.unmount()
    expect(loadDraft(scope)?.formValues.name).toBe("Pendiente")
  })
})
