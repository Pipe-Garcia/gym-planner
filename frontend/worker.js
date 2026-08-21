const SECURITY_HEADERS = {
  "Strict-Transport-Security": "max-age=31536000",
  "X-Content-Type-Options": "nosniff",
  "X-Frame-Options": "DENY",
  "Referrer-Policy": "strict-origin-when-cross-origin",
  "Permissions-Policy": "camera=(), microphone=(), geolocation=()",
}

function originalProtocol(request, url) {
  const forwardedProtocol = request.headers.get("X-Forwarded-Proto")?.toLowerCase()
  return forwardedProtocol === "http" || forwardedProtocol === "https"
    ? forwardedProtocol
    : url.protocol.slice(0, -1)
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url)

    if (originalProtocol(request, url) === "http") {
      url.protocol = "https:"
      return Response.redirect(url.toString(), 308)
    }

    const assetResponse = await env.ASSETS.fetch(request)
    const securedResponse = new Response(assetResponse.body, assetResponse)

    for (const [name, value] of Object.entries(SECURITY_HEADERS)) {
      securedResponse.headers.set(name, value)
    }

    return securedResponse
  },
}
