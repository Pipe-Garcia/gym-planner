import { useCallback, useEffect, useRef, useState } from "react"
import type { UseFormReturn } from "react-hook-form"
import { registerDraftPersistence, removeDraft, saveDraft, type DraftScope } from "@/lib/routine-draft"
import type { RoutineFormValues } from "@/schemas/template.schema"

type DraftStatus = "clean" | "unsaved" | "protected"
const DEBOUNCE_MS = 600

export function useRoutineDraft(form: UseFormReturn<RoutineFormValues>, scope: DraftScope | null, selectedDay: number, enabled: boolean) {
  const [status, setStatus] = useState<DraftStatus>("clean")
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const dirty = useRef(false)
  const stopped = useRef(false)
  const latestDay = useRef(selectedDay)
  latestDay.current = selectedDay

  const cancelTimer = useCallback(() => {
    if (timer.current) clearTimeout(timer.current)
    timer.current = null
  }, [])

  const flush = useCallback(() => {
    cancelTimer()
    if (!scope || !enabled || stopped.current || !dirty.current) return
    setStatus(saveDraft(scope, form.getValues(), latestDay.current) ? "protected" : "unsaved")
  }, [cancelTimer, enabled, form, scope])

  const stopPersistence = useCallback(() => {
    stopped.current = true
    cancelTimer()
  }, [cancelTimer])

  const clearOnSuccess = useCallback(() => {
    stopPersistence()
    if (scope) removeDraft(scope)
    setStatus("clean")
  }, [scope, stopPersistence])

  useEffect(() => {
    if (!scope || !enabled) return
    stopped.current = false
    dirty.current = form.formState.isDirty
    const unregisterStop = registerDraftPersistence(scope, stopPersistence)
    const schedule = () => {
      cancelTimer()
      setStatus("unsaved")
      timer.current = setTimeout(flush, DEBOUNCE_MS)
    }
    const unsubscribe = form.subscribe({
      formState: { values: true, isDirty: true },
      callback: ({ isDirty }) => {
        const wasDirty = dirty.current
        dirty.current = Boolean(isDirty)
        if (isDirty) schedule()
        else {
          cancelTimer()
          if (wasDirty) removeDraft(scope)
          setStatus("clean")
        }
      },
    })
    const onVisibility = () => { if (document.visibilityState === "hidden") flush() }
    const onPageHide = () => flush()
    document.addEventListener("visibilitychange", onVisibility)
    window.addEventListener("pagehide", onPageHide)
    return () => {
      unregisterStop()
      if (dirty.current) flush()
      cancelTimer()
      unsubscribe()
      document.removeEventListener("visibilitychange", onVisibility)
      window.removeEventListener("pagehide", onPageHide)
    }
  }, [scope, enabled, form, cancelTimer, flush, stopPersistence])

  useEffect(() => {
    if (enabled && dirty.current && !stopped.current) {
      cancelTimer()
      setStatus("unsaved")
      timer.current = setTimeout(flush, DEBOUNCE_MS)
    }
  }, [selectedDay, enabled, cancelTimer, flush])

  return { status, clearOnSuccess, flush }
}
