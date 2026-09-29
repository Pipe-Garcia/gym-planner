import { z } from "zod"
import { routineDraftFormSchema, type RoutineFormValues } from "@/schemas/template.schema"

export const ROUTINE_DRAFT_PREFIX = "gym_planner:routine-draft:"
export const ROUTINE_DRAFT_TTL_MS = 24 * 60 * 60 * 1000
const VERSION = 1

export type DraftScope = { gymId: number; userId: number; routineId: number }
type StopDraftPersistence = () => void
const activeDraftPersistence = new Map<string, Set<StopDraftPersistence>>()
const envelopeSchema = z.object({
  version: z.literal(1),
  gymId: z.number().int().positive(),
  userId: z.number().int().positive(),
  routineId: z.number().int().positive(),
  savedAt: z.number().int().nonnegative(),
  formValues: routineDraftFormSchema,
  selectedDay: z.number().int().nonnegative(),
})
export type RoutineDraftEnvelope = z.infer<typeof envelopeSchema>

export function draftKey(scope: DraftScope) {
  return `${ROUTINE_DRAFT_PREFIX}v${VERSION}:${scope.gymId}:${scope.userId}:${scope.routineId}`
}

function userScopeKey(gymId: number, userId: number) {
  return `${gymId}:${userId}`
}

export function registerDraftPersistence(scope: DraftScope, stop: StopDraftPersistence): () => void {
  const key = userScopeKey(scope.gymId, scope.userId)
  const stops = activeDraftPersistence.get(key) ?? new Set<StopDraftPersistence>()
  stops.add(stop)
  activeDraftPersistence.set(key, stops)
  return () => {
    stops.delete(stop)
    if (stops.size === 0) activeDraftPersistence.delete(key)
  }
}

export function stopUserDraftPersistence(gymId: number, userId: number): void {
  const key = userScopeKey(gymId, userId)
  const stops = activeDraftPersistence.get(key)
  if (!stops) return
  for (const stop of stops) stop()
  activeDraftPersistence.delete(key)
}

function storage(): Storage | null {
  try { return window.localStorage } catch { return null }
}

export function saveDraft(scope: DraftScope, formValues: RoutineFormValues, selectedDay: number): boolean {
  try {
    const target = storage()
    if (!target) return false
    const parsed = envelopeSchema.safeParse({ version: VERSION, ...scope, savedAt: Date.now(), formValues, selectedDay })
    if (!parsed.success) return false
    target.setItem(draftKey(scope), JSON.stringify(parsed.data))
    return true
  } catch { return false }
}

export function removeDraft(scope: DraftScope): boolean {
  try {
    const target = storage()
    if (!target) return false
    target.removeItem(draftKey(scope))
    return true
  } catch { return false }
}

function parseDraft(raw: string | null, scope: DraftScope, now: number): RoutineDraftEnvelope | null {
  if (!raw) return null
  try {
    const parsed = envelopeSchema.safeParse(JSON.parse(raw))
    if (!parsed.success) return null
    const draft = parsed.data
    if (draft.gymId !== scope.gymId || draft.userId !== scope.userId || draft.routineId !== scope.routineId) return null
    if (draft.savedAt > now || now - draft.savedAt >= ROUTINE_DRAFT_TTL_MS) return null
    return draft
  } catch { return null }
}

export function loadDraft(scope: DraftScope, now = Date.now()): RoutineDraftEnvelope | null {
  try {
    const raw = storage()?.getItem(draftKey(scope)) ?? null
    const draft = parseDraft(raw, scope, now)
    if (raw && !draft) removeDraft(scope)
    return draft
  } catch { return null }
}

export function cleanupDrafts(now = Date.now()): void {
  try {
    const target = storage()
    if (!target) return
    for (let index = target.length - 1; index >= 0; index--) {
      const key = target.key(index)
      if (!key?.startsWith(ROUTINE_DRAFT_PREFIX)) continue
      const match = /^gym_planner:routine-draft:v1:(\d+):(\d+):(\d+)$/.exec(key)
      const scope = match && { gymId: Number(match[1]), userId: Number(match[2]), routineId: Number(match[3]) }
      if (!scope || !parseDraft(target.getItem(key), scope, now)) target.removeItem(key)
    }
  } catch { /* Storage may be blocked. The editor remains usable. */ }
}

export function removeUserDrafts(gymId: number, userId: number): void {
  try {
    const target = storage()
    if (!target) return
    const prefix = `${ROUTINE_DRAFT_PREFIX}v${VERSION}:${gymId}:${userId}:`
    for (let index = target.length - 1; index >= 0; index--) {
      const key = target.key(index)
      if (key?.startsWith(prefix)) target.removeItem(key)
    }
  } catch { /* Logout must still succeed if storage is unavailable. */ }
}

export function sameEditableRoutine(a: RoutineFormValues, b: RoutineFormValues): boolean {
  const canonical = (value: unknown): unknown => {
    if (Array.isArray(value)) return value.map(canonical)
    if (value && typeof value === "object") {
      return Object.fromEntries(Object.entries(value)
        .filter(([key, item]) => key !== "id" && key !== "setNumber" && item !== null && item !== undefined && item !== "")
        .sort(([aKey], [bKey]) => aKey.localeCompare(bKey))
        .map(([key, item]) => [key, canonical(item)]))
    }
    return value
  }
  return JSON.stringify(canonical(a)) === JSON.stringify(canonical(b))
}
