import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { cleanupDrafts, draftKey, loadDraft, removeDraft, removeUserDrafts, ROUTINE_DRAFT_TTL_MS, saveDraft, sameEditableRoutine } from "./routine-draft"
import { defaultDay, emptySet } from "@/components/template/formDefaults"
import { routineFormSchema, type RoutineFormValues } from "@/schemas/template.schema"

const scope = { gymId: 1, userId: 2, routineId: 3 }
const values: RoutineFormValues = {
  name: "Rutina", description: null, sport: null, objective: null, level: null,
  generalNotes: null, internalNotes: "Privado", assignedDate: "2026-09-25", status: "DRAFT",
  days: [defaultDay(1)],
}

beforeEach(() => { localStorage.clear(); vi.useFakeTimers(); vi.setSystemTime(new Date("2026-09-25T12:00:00Z")) })
afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks() })

describe("routine draft storage", () => {
  it("saves and loads scoped values and selected day", () => {
    expect(saveDraft(scope, values, 0)).toBe(true)
    expect(loadDraft(scope)?.formValues.internalNotes).toBe("Privado")
    expect(loadDraft(scope)?.selectedDay).toBe(0)
    expect(loadDraft({ ...scope, userId: 9 })).toBeNull()
    expect(loadDraft({ ...scope, gymId: 9 })).toBeNull()
    expect(loadDraft({ ...scope, routineId: 9 })).toBeNull()
  })

  it("expires at 24h without deleting a live form", () => {
    saveDraft(scope, values, 0)
    vi.advanceTimersByTime(ROUTINE_DRAFT_TTL_MS)
    expect(loadDraft(scope)).toBeNull()
    expect(localStorage.getItem(draftKey(scope))).toBeNull()
  })

  it("rejects corrupt, invalid or incompatible entries", () => {
    localStorage.setItem(draftKey(scope), "{")
    expect(loadDraft(scope)).toBeNull()
    localStorage.setItem(draftKey(scope), JSON.stringify({ version: 2 }))
    expect(loadDraft(scope)).toBeNull()
    localStorage.setItem(draftKey(scope), JSON.stringify({ version: 1, ...scope, savedAt: Date.now(), formValues: { name: 8 }, selectedDay: 0 }))
    expect(loadDraft(scope)).toBeNull()
  })

  it("accepts a temporarily blank block title", () => {
    const incomplete = { ...values, days: [{ ...defaultDay(1), blocks: [{ orderIndex: 1, title: "", structuralType: "STANDARD" as const, purpose: null, totalDurationSeconds: null, targetRounds: null, roundRestSeconds: null, blockNotes: null, exercises: [] }] }] }
    expect(saveDraft(scope, incomplete, 0)).toBe(true)
    expect(loadDraft(scope)?.formValues.days[0].blocks[0].title).toBe("")
  })

  it("persists the complete current snapshot through legitimate transient editor states", () => {
    expect(saveDraft(scope, values, 0)).toBe(true)
    const transient: RoutineFormValues = {
      ...values,
      name: "",
      assignedDate: "",
      internalNotes: "Cambio posterior",
      days: [{
        ...defaultDay(1),
        name: "",
        blocks: [{
          orderIndex: 1,
          title: "",
          structuralType: "STANDARD",
          purpose: null,
          totalDurationSeconds: null,
          targetRounds: null,
          roundRestSeconds: null,
          blockNotes: null,
          exercises: [{
            exerciseId: 10,
            exerciseName: "Press",
            exerciseMeasurement: "REPS_WEIGHT",
            orderIndex: 1,
            exerciseNotes: null,
            sets: [{
              ...emptySet(1),
              setNumber: 1,
              setKind: "NORMAL",
              targetReps: 0,
              targetRepsMin: 0,
              targetRepsMax: -1,
              targetWeightKg: 0,
              targetTimeSeconds: 0,
              targetDistanceMeters: -1.5,
              restAfterSeconds: -1,
              tempo: "",
              executionCue: "",
              rpe: 0,
              notes: "",
              toFailure: false,
            }],
          }],
        }],
      }],
    }
    expect(saveDraft(scope, transient, 0)).toBe(true)
    expect(loadDraft(scope)?.formValues).toMatchObject({
      name: "",
      assignedDate: "",
      internalNotes: "Cambio posterior",
      days: [{ name: "", blocks: [{ title: "", exercises: [{ sets: [{ targetWeightKg: 0, targetReps: 0, targetDistanceMeters: -1.5 }] }] }] }],
    })
  })

  it("keeps structural validation and the final submit validation strict", () => {
    expect(saveDraft(scope, { ...values, days: [{ ...defaultDay(1), blocks: [{ orderIndex: 1, title: "", structuralType: "INVALID" }] }] } as unknown as RoutineFormValues, 0)).toBe(false)
    expect(routineFormSchema.safeParse({ ...values, assignedDate: "" }).success).toBe(false)
    const invalidSubmit = {
      ...values,
      days: [{ ...defaultDay(1), blocks: [{
        orderIndex: 1, title: "Bloque", structuralType: "STANDARD" as const, purpose: null,
        totalDurationSeconds: null, targetRounds: null, roundRestSeconds: null, blockNotes: null,
        exercises: [{ exerciseId: 10, exerciseName: "Press", exerciseMeasurement: "REPS_WEIGHT" as const, orderIndex: 1, exerciseNotes: null, sets: [{
          setNumber: 1, setKind: "NORMAL" as const, targetReps: 10, targetRepsMin: null, targetRepsMax: null,
          targetWeightKg: 0, targetTimeSeconds: null, targetDistanceMeters: null, restAfterSeconds: 60,
          tempo: null, executionCue: null, rpe: null, notes: null, toFailure: false,
        }] }],
      }] }],
    }
    expect(routineFormSchema.safeParse(invalidSubmit).success).toBe(false)
  })

  it("clears one draft or only the current user's drafts", () => {
    saveDraft(scope, values, 0)
    saveDraft({ ...scope, routineId: 4 }, values, 0)
    saveDraft({ ...scope, userId: 9 }, values, 0)
    expect(removeDraft(scope)).toBe(true)
    expect(loadDraft(scope)).toBeNull()
    removeUserDrafts(scope.gymId, scope.userId)
    expect(loadDraft({ ...scope, routineId: 4 })).toBeNull()
    expect(loadDraft({ ...scope, userId: 9 })).not.toBeNull()
  })

  it("cleans expired and invalid keys only under its own prefix", () => {
    saveDraft(scope, values, 0)
    localStorage.setItem("unrelated", "keep")
    localStorage.setItem("gym_planner:routine-draft:v99:1:2:3", "bad")
    cleanupDrafts(Date.now() + ROUTINE_DRAFT_TTL_MS)
    expect(localStorage.getItem(draftKey(scope))).toBeNull()
    expect(localStorage.getItem("gym_planner:routine-draft:v99:1:2:3")).toBeNull()
    expect(localStorage.getItem("unrelated")).toBe("keep")
  })

  it("handles setItem and storage access failures", () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementationOnce(() => { throw Error("quota") })
    expect(saveDraft(scope, values, 0)).toBe(false)
    const getter = vi.spyOn(window, "localStorage", "get").mockImplementation(() => { throw Error("blocked") })
    expect(saveDraft(scope, values, 0)).toBe(false)
    expect(loadDraft(scope)).toBeNull()
    expect(() => cleanupDrafts()).not.toThrow()
    getter.mockRestore()
  })

  it("ignores technical IDs but retains editable internal notes", () => {
    expect(sameEditableRoutine(values, { ...values, days: [{ ...values.days[0], id: 44 }] })).toBe(true)
    expect(sameEditableRoutine(values, { ...values, internalNotes: "changed" })).toBe(false)
  })
})
