import * as React from "react"
import { cn } from "@/lib/utils"

export function TooltipProvider({ children }: { children: React.ReactNode; delayDuration?: number }) {
  return <>{children}</>
}

export function Tooltip({ children }: { children: React.ReactNode }) {
  return <span className="group/tooltip relative inline-flex">{children}</span>
}

export function TooltipTrigger({ children, asChild }: { children: React.ReactElement; asChild?: boolean }) {
  if (asChild) {
    return children
  }
  return <span>{children}</span>
}

export function TooltipContent({ children, className, side = "top" }: { children: React.ReactNode; className?: string; side?: "top" | "right" | "bottom" | "left" }) {
  return (
    <span
      className={cn(
        "pointer-events-none absolute z-50 hidden rounded-md border bg-white px-3 py-2 text-xs text-foreground shadow-md group-hover/tooltip:block group-focus-within/tooltip:block",
        side === "left" && "right-12 top-1/2 -translate-y-1/2",
        side === "right" && "left-12 top-1/2 -translate-y-1/2",
        side === "bottom" && "left-1/2 top-12 -translate-x-1/2",
        side === "top" && "bottom-12 left-1/2 -translate-x-1/2",
        className,
      )}
      role="tooltip"
    >
      {children}
    </span>
  )
}
