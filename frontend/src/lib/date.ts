export function formatDateEs(value?: string | null): string {
  const date = parseDate(value)
  if (!date) return "-"
  const day = String(date.getDate()).padStart(2, "0")
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const year = date.getFullYear()
  return `${day}/${month}/${year}`
}

export function toDateInputValue(value?: string | null): string {
  if (!value) return ""
  return value.slice(0, 10)
}

export function fromDateInputValue(value: string): string | null {
  return value ? value.slice(0, 10) : null
}

function parseDate(value?: string | null): Date | null {
  if (!value) return null
  const raw = value.slice(0, 10)
  const [year, month, day] = raw.split("-").map(Number)
  if (!year || !month || !day) return null
  return new Date(year, month - 1, day)
}
