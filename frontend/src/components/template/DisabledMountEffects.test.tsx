import { render, screen } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"
import { FormProvider, useForm } from "react-hook-form"
import { BlockEditor } from "./BlockEditor"
import { CompactTargetExerciseRow } from "./ExerciseInBlockRow"
import { defaultDay, emptyBlock } from "./formDefaults"

vi.mock("./ExercisePicker", () => ({ ExercisePicker: () => null }))

describe("disabled editor mount effects", () => {
  it("does not normalize block values while disabled", () => {
    let read = () => ({ totalDurationSeconds: 0, targetRounds: 0, roundRestSeconds: 0 })
    function Fixture() {
      const form = useForm({ defaultValues: { days: [{ ...defaultDay(1), blocks: [{ ...emptyBlock(1), totalDurationSeconds: 120, targetRounds: 4, roundRestSeconds: 30 }] }] } })
      read = () => form.getValues("days.0.blocks.0")
      return <FormProvider {...form}><BlockEditor blockIndex={0} blockPath="days.0.blocks.0" blocksLength={1} disabled onRemove={() => {}} onMoveUp={() => {}} onMoveDown={() => {}} /><output>{form.formState.isDirty ? "dirty" : "clean"}</output></FormProvider>
    }
    render(<Fixture />)
    expect(read().totalDurationSeconds).toBe(120)
    expect(read().targetRounds).toBe(4)
    expect(read().roundRestSeconds).toBe(30)
    expect(screen.getByRole("status")).toHaveTextContent("clean")
  })

  it("does not initialize empty compact sets while disabled", () => {
    let read = (): unknown[] => []
    function Fixture() {
      const form = useForm({ defaultValues: { exercise: { sets: [] as unknown[] } } })
      read = () => form.getValues("exercise.sets")
      return <FormProvider {...form}><CompactTargetExerciseRow prefix="exercise" measurement="REPS_ONLY" context="routine" disabled /></FormProvider>
    }
    render(<Fixture />)
    expect(read()).toEqual([])
  })
})
