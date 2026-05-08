import { format } from "date-fns"
import { es } from "date-fns/locale"

export function formatDateTime(value: string | Date) {
  return format(new Date(value), "dd/MM/yyyy HH:mm", { locale: es })
}
