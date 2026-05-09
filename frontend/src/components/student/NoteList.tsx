import { Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import type { StudentNote } from "@/types/student"

interface NoteListProps {
  notes: StudentNote[]
  onDelete: (note: StudentNote) => void
}

export function NoteList({ notes, onDelete }: NoteListProps) {
  if (notes.length === 0) {
    return <div className="rounded-md border bg-white p-6 text-sm text-muted-foreground">Todavia no hay notas internas.</div>
  }

  return (
    <div className="space-y-3">
      {notes.map((note) => (
        <article key={note.id} className="rounded-md border bg-white p-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-sm font-medium">{note.authorName}</p>
              <p className="text-xs text-muted-foreground">{new Date(note.createdAt).toLocaleString()}</p>
            </div>
            <Button type="button" variant="ghost" size="icon" onClick={() => onDelete(note)} aria-label="Eliminar nota">
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
          <p className="mt-3 whitespace-pre-wrap text-sm">{note.content}</p>
        </article>
      ))}
    </div>
  )
}
