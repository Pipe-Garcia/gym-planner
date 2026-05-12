import { ArrowDown, ArrowUp } from "lucide-react"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

interface Props { onMoveUp: () => void; onMoveDown: () => void; disableUp: boolean; disableDown: boolean; disabled?: boolean; orientation?: "vertical" | "horizontal" | "responsive" }

export function ReorderButtons({ onMoveUp, onMoveDown, disableUp, disableDown, disabled, orientation = "vertical" }: Props) {
  return (
    <div className={cn("flex gap-1", orientation === "vertical" && "flex-col", orientation === "horizontal" && "flex-row", orientation === "responsive" && "flex-row sm:flex-col")}>
      <Button type="button" size="icon" variant="ghost" onClick={onMoveUp} disabled={disabled || disableUp} aria-label="Subir">
        <ArrowUp className="h-4 w-4" />
      </Button>
      <Button type="button" size="icon" variant="ghost" onClick={onMoveDown} disabled={disabled || disableDown} aria-label="Bajar">
        <ArrowDown className="h-4 w-4" />
      </Button>
    </div>
  )
}
