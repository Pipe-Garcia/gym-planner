import type { BlockPurpose } from "@/types/training"

export type SectionGroup = "warmup" | "main" | "cooldown"

export function sectionGroupFromPurpose(purpose?: BlockPurpose | null): SectionGroup {
  if (purpose === "WARMUP" || purpose === "ACTIVATION") return "warmup"
  if (purpose === "COOLDOWN") return "cooldown"
  return "main"
}

export function sectionStyleByGroup(group: SectionGroup) {
  const styles: Record<SectionGroup, string> = {
    warmup: "border-amber-200 bg-amber-50/40",
    main: "border-teal-200 bg-teal-50/40",
    cooldown: "border-indigo-200 bg-indigo-50/40",
  }
  return styles[group]
}

export function sectionIconStyleByGroup(group: SectionGroup) {
  const styles: Record<SectionGroup, string> = {
    warmup: "bg-amber-100 text-amber-700",
    main: "bg-teal-100 text-teal-700",
    cooldown: "bg-indigo-100 text-indigo-700",
  }
  return styles[group]
}
