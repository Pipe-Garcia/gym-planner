import { Search } from "lucide-react"
import { Input } from "@/components/ui/input"

interface StudentSearchBarProps {
  value: string
  onChange: (value: string) => void
}

export function StudentSearchBar({ value, onChange }: StudentSearchBarProps) {
  return (
    <div className="relative">
      <Search className="pointer-events-none absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
      <Input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder="Buscar por nombre, DNI o teléfono"
        className="pl-10"
      />
    </div>
  )
}
