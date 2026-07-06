import { toast, type ExternalToast } from "sonner"

type ToastMessage = Parameters<typeof toast.success>[0]

function withDuration(options: ExternalToast | undefined, duration: number): ExternalToast {
  return { duration, ...options }
}

export function useToast() {
  return {
    success: (message: ToastMessage, options?: ExternalToast) => toast.success(message, withDuration(options, 4000)),
    error: (message: ToastMessage, options?: ExternalToast) => toast.error(message, withDuration(options, 5500)),
    info: (message: ToastMessage, options?: ExternalToast) => toast.info(message, withDuration(options, 4000)),
  }
}
