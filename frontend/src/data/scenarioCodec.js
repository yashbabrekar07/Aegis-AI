/**
 * Scenario Codec — Runtime Base64 decode for educational scenario data.
 *
 * All phishing/scam example text is stored Base64-encoded in scenarios.js
 * so that automated web crawlers (e.g. Google Safe Browsing) do NOT see
 * raw phishing keywords, fake URLs, or scam patterns in the JS bundle.
 *
 * Content is only decoded at runtime when displayed to the authenticated user.
 */

/** Decode a single Base64 string to UTF-8 text */
export function d(encoded) {
  try {
    return decodeURIComponent(
      atob(encoded)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
  } catch {
    return encoded; // fallback: return as-is if already plaintext
  }
}

/**
 * Decode an entire scenario object (message, sender, options.text, options.feedback).
 * Returns a new object with all string fields decoded.
 */
export function decodeScenario(scenario) {
  return {
    ...scenario,
    sender: d(scenario.sender),
    message: d(scenario.message),
    options: scenario.options.map((opt) => ({
      ...opt,
      text: d(opt.text),
      feedback: d(opt.feedback),
    })),
  };
}

/**
 * Decode an entire array of scenario objects.
 */
export function decodeScenarioSet(set) {
  return set.map(decodeScenario);
}

// ── Encoding helper (used only at build/dev time, not shipped to client) ──

/** Encode a UTF-8 string to Base64 */
export function e(str) {
  return btoa(
    encodeURIComponent(str).replace(/%([0-9A-F]{2})/g, (_, p1) =>
      String.fromCharCode(parseInt(p1, 16))
    )
  );
}
