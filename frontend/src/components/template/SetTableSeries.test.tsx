import { render, screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it, vi } from "vitest"
import { FormProvider, useForm } from "react-hook-form"
import { SetTable } from "./SetTable"
import { emptySet } from "./formDefaults"
import type { ExerciseSetInput } from "@/types/training"

function makeSets(count: number): ExerciseSetInput[] {
  return Array.from({ length: count }, (_, index) => ({ ...emptySet(index + 1), targetReps: 10 }))
}

function renderEditor(count = 3, disabled = false) {
  const saved = vi.fn()
  let read = (): ExerciseSetInput[] => []
  function Fixture() {
    const form = useForm({ defaultValues: { sets: makeSets(count) } })
    read = () => form.getValues("sets")
    return <FormProvider {...form}>
      <form onSubmit={form.handleSubmit((values) => saved(values.sets))}>
        <SetTable name="sets" measurement="REPS_ONLY" context="routine" disabled={disabled} />
        <button type="submit">Guardar</button>
      </form>
    </FormProvider>
  }
  render(<Fixture />)
  return { read: () => read(), saved }
}

describe("Series buffer", () => {
  it("keeps sets while empty, then appends neutral sets only on blur", async () => {
    const user = userEvent.setup()
    const { read } = renderEditor()
    const series = screen.getByRole("spinbutton", { name: "Series" })
    await user.clear(series)
    expect(series).toHaveValue(null)
    expect(read()).toHaveLength(3)
    await user.type(series, "5")
    expect(read()).toHaveLength(3)
    await user.tab()
    await waitFor(() => expect(read()).toHaveLength(5))
    expect(read().map((set) => set.setNumber)).toEqual([1, 2, 3, 4, 5])
    expect(read().slice(0, 3).map((set) => set.targetReps)).toEqual([10, 10, 10])
    expect(read()[4].rpe).toBeNull()
  })

  it("removes only the suffix and commits Enter", async () => {
    const user = userEvent.setup()
    const { read } = renderEditor(5)
    const originals = read().slice(0, 3)
    const series = screen.getByRole("spinbutton", { name: "Series" })
    await user.clear(series)
    await user.type(series, "3{Enter}")
    expect(read()).toHaveLength(3)
    expect(read()).toEqual(originals)
  })

  it("commits valid Series before the immediate Save snapshot", async () => {
    const user = userEvent.setup()
    const { saved } = renderEditor(1)
    const series = screen.getByRole("spinbutton", { name: "Series" })
    await user.clear(series)
    await user.type(series, "8")
    await user.click(screen.getByRole("button", { name: "Guardar" }))
    await waitFor(() => expect(saved).toHaveBeenCalled())
    expect(saved.mock.calls[0][0]).toHaveLength(8)
  })

  it("disables both mode controls when editing is disabled", () => {
    renderEditor(1, true)
    expect(screen.getByRole("button", { name: "Simple" })).toBeDisabled()
    expect(screen.getByRole("button", { name: "Avanzado" })).toBeDisabled()
  })

  it("renumbers remaining sets after deleting a middle row in Advanced", async () => {
    const user = userEvent.setup()
    const { read } = renderEditor(3)
    await user.click(screen.getByRole("button", { name: "Avanzado" }))
    await user.click(screen.getByRole("button", { name: "Eliminar serie 2" }))
    expect(read().map((set) => set.setNumber)).toEqual([1, 2])
  })

  it("updates one simple property across existing sets without leaving Simple", async () => {
    const user = userEvent.setup()
    const { read } = renderEditor(3)
    const reps = screen.getAllByRole("spinbutton", { name: "Reps" })[0]
    await user.clear(reps)
    await user.type(reps, "12")
    expect(read().map((set) => set.targetReps)).toEqual([12, 12, 12])
    expect(read().map((set) => set.setNumber)).toEqual([1, 2, 3])
    expect(screen.getByRole("button", { name: "Simple" })).not.toBeDisabled()
  })

  it("keeps three sets while typing 12 and preserves them when the value commits", async () => {
    const user = userEvent.setup()
    const { read } = renderEditor(3)
    const originals = read().slice(0, 3)
    const series = screen.getByRole("spinbutton", { name: "Series" })
    await user.clear(series)
    await user.type(series, "12")
    expect(read()).toHaveLength(3)
    expect(read()).toEqual(originals)
    await user.tab()
    await waitFor(() => expect(read()).toHaveLength(12))
    expect(read().slice(0, 3)).toEqual(originals)
  })
})
