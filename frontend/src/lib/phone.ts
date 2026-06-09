const ARGENTINA_COUNTRY_CODE = "54"
const ARGENTINA_MOBILE_PREFIX = "9"
const ARGENTINA_NATIONAL_NUMBER_LENGTH = 10

export function normalizePhoneForWhatsApp(
  phone: string | null | undefined,
): string | null {
  let digits = phone?.replace(/\D/g, "") ?? ""
  if (!digits) return null

  // Accept an international prefix, then normalize the remaining Argentine number.
  if (digits.startsWith("00")) digits = digits.slice(2)
  if (digits.startsWith(ARGENTINA_COUNTRY_CODE)) {
    digits = digits.slice(ARGENTINA_COUNTRY_CODE.length)
  }

  // Argentine mobile numbers may be stored with a trunk 0, a mobile 9, or both.
  if (digits.startsWith("0")) digits = digits.slice(1)
  if (
    digits.startsWith(ARGENTINA_MOBILE_PREFIX) &&
    digits.length === ARGENTINA_NATIONAL_NUMBER_LENGTH + 1
  ) {
    digits = digits.slice(1)
  }

  // Legacy local format: area code + 15 + subscriber number.
  if (digits.length === ARGENTINA_NATIONAL_NUMBER_LENGTH + 2) {
    for (let areaCodeLength = 2; areaCodeLength <= 4; areaCodeLength += 1) {
      if (digits.slice(areaCodeLength, areaCodeLength + 2) === "15") {
        digits =
          digits.slice(0, areaCodeLength) + digits.slice(areaCodeLength + 2)
        break
      }
    }
  }

  if (
    digits.length !== ARGENTINA_NATIONAL_NUMBER_LENGTH ||
    /^(\d)\1+$/.test(digits)
  ) {
    return null
  }

  return `${ARGENTINA_COUNTRY_CODE}${ARGENTINA_MOBILE_PREFIX}${digits}`
}

export function buildWhatsAppUrl(
  normalizedPhone: string,
  message: string,
  userAgent: string,
): string {
  const encodedMessage = encodeURIComponent(message)
  const isMobile = /Android|iPhone|iPad/i.test(userAgent)

  // Desktop goes straight to WhatsApp Web; mobile uses wa.me to open the native app.
  return isMobile
    ? `https://wa.me/${normalizedPhone}?text=${encodedMessage}`
    : `https://web.whatsapp.com/send?phone=${normalizedPhone}&text=${encodedMessage}`
}
