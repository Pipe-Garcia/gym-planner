import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it } from "vitest"
import { FormProvider, useForm } from "react-hook-form"
import { SetTable } from "./SetTable"
import { emptySet } from "./formDefaults"

let readRpes = (): Array<number | null> => []
function Fixture() {
  const form = useForm({
    defaultValues: {
      sets: [7, 8, 9].map((rpe, index) => ({
        ...emptySet(index + 1),
        targetReps: 10,
        rpe,
      })),
    },
  })
  readRpes = () => form.getValues("sets").map((set) => set.rpe)
  return (
    <FormProvider {...form}>
      <SetTable name="sets" measurement="REPS_ONLY" context="routine" />
    </FormProvider>
  )
}

describe("SetTable", () => {
  it("does not let Simple erase heterogeneous RPE values", async () => {
    const user = userEvent.setup()
    render(<Fixture />)
    const simple = screen.getByRole("button", { name: "Simple" })
    // On the old implementation this branch was reachable and editing Reps
    // rebuilt all sets, silently erasing RPE 7/8/9.
    if (!simple.hasAttribute("disabled")) {
      const reps = screen.getByRole("spinbutton", { name: "Reps" })
      await user.clear(reps)
      await user.type(reps, "12")
    }
    expect(readRpes()).toEqual([7, 8, 9])
    expect(simple).toBeDisabled()
  })

  it("switches away from Simple when advanced data appears and never switches back automatically", async () => {
    const user = userEvent.setup()
    function ReactiveFixture() {
      const form = useForm({ defaultValues: { sets: [{ ...emptySet(1), targetReps: 10 }] } })
      return <FormProvider {...form}>
        <SetTable name="sets" measurement="REPS_ONLY" context="routine" />
        <button onClick={() => form.setValue("sets.0.rpe", 8, { shouldDirty: true })}>Agregar RPE</button>
        <button onClick={() => form.setValue("sets.0.rpe", null, { shouldDirty: true })}>Quitar RPE</button>
      </FormProvider>
    }
    render(<ReactiveFixture />)
    expect(screen.getByRole("button", { name: "Simple" })).not.toBeDisabled()
    await user.click(screen.getByRole("button", { name: "Agregar RPE" }))
    expect(screen.getByRole("button", { name: "Simple" })).toBeDisabled()
    expect(screen.getByText(/Estas series usan opciones/)).toBeInTheDocument()
    await user.click(screen.getByRole("button", { name: "Quitar RPE" }))
    expect(screen.getByRole("button", { name: "Simple" })).not.toBeDisabled()
    expect(screen.getByRole("button", { name: "Avanzado" }).className).toContain("bg-primary")
  })
})
