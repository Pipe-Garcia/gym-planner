import { render, screen } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import { describe, expect, it } from "vitest"
import { FormProvider, useForm } from "react-hook-form"
import { TrainingDaysEditor } from "./TrainingDaysEditor"
import { defaultDay } from "./formDefaults"

function Fixture({ context }: { context: "template" | "routine" }) {
  const form = useForm({ defaultValues: { days: [defaultDay(1), defaultDay(2)] } })
  return <FormProvider {...form}><TrainingDaysEditor context={context} /></FormProvider>
}

describe.each(["template", "routine"] as const)("TrainingDaysEditor %s", (context) => {
  it("shows the correct name when switching days and keeps edits separate", async () => {
    const user = userEvent.setup()
    render(<Fixture context={context} />)
    expect(screen.getByRole("textbox", { name: "Dia" })).toHaveValue("Día 1")
    await user.click(screen.getByRole("button", { name: "Día 2" }))
    expect(screen.getByRole("textbox", { name: "Dia" })).toHaveValue("Día 2")
    await user.clear(screen.getByRole("textbox", { name: "Dia" }))
    await user.type(screen.getByRole("textbox", { name: "Dia" }), "Piernas")
    await user.click(screen.getByRole("button", { name: "Día 1" }))
    expect(screen.getByRole("textbox", { name: "Dia" })).toHaveValue("Día 1")
    await user.click(screen.getByRole("button", { name: "Piernas" }))
    expect(screen.getByRole("textbox", { name: "Dia" })).toHaveValue("Piernas")
  })

  it("keeps names distinct through add, duplicate and remove", async () => {
    const user = userEvent.setup()
    render(<Fixture context={context} />)
    await user.click(screen.getByRole("button", { name: /Agregar dia/i }))
    expect(screen.getByRole("textbox", { name: "Dia" })).toHaveValue("Día 3")
    await user.click(screen.getByRole("button", { name: /Duplicar dia/i }))
    expect(screen.getByRole("textbox", { name: "Dia" })).toHaveValue("Día 3 (copia)")
    await user.click(screen.getByRole("button", { name: /Eliminar Día 3 \(copia\)/i }))
    expect(screen.getByRole("textbox", { name: "Dia" })).toHaveValue("Día 3")
  })
})
