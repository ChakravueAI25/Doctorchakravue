package com.org.doctorchakravue.app

/**
 * Central legal / consent configuration.
 *
 * ⚠️ EDIT THESE WHEN THE HOSTING OR URLS CHANGE — this is the single place.
 * Both the doctor and patient apps currently point at the same chakravue.ai URLs;
 * switch to app-specific paths here when ready.
 */
object LegalConfig {
    /** Full Terms of Service (opened in the browser from the consent screen). */
    const val TERMS_URL = "https://www.chakravue.ai/terms"

    /** Full Privacy Policy. */
    const val PRIVACY_URL = "https://www.chakravue.ai/privacy-policy"

    /**
     * Bump this whenever the Terms/Privacy text changes — every user will then be
     * asked to re-accept the new version on their next login.
     */
    const val TERMS_VERSION = 1

    /** Short in-app summary (the binding full text lives at the URLs above). */
    const val SUMMARY = """ChakraVue Doctor is intended for qualified, registered healthcare professionals.

• Clinical responsibility: The app provides informational assistance only. It does not diagnose or make clinical decisions — you remain solely responsible for all clinical judgments, diagnoses, prescriptions and patient care.

• Patient data & confidentiality: Access patient information only for legitimate clinical purposes, maintain confidentiality, and comply with applicable data-protection law (Digital Personal Data Protection Act, 2023).

• Your account: Provide accurate professional details, keep your credentials confidential, and accept responsibility for activity under your account.

• Acceptable use: Do not attempt unauthorised access, interfere with the app, or reverse-engineer it.

• Third-party services: The app uses Google Firebase (notifications) and Agora (video/audio calls); your use of those features is also subject to their terms.

By continuing you confirm you are a licensed healthcare professional and agree to the full Terms of Service and Privacy Policy."""
}
