"""
Diagnostic script to test the /call/token endpoint and verify
what the patient backend actually returns for the Agora App ID.

Usage:
    pip install httpx
    python test_videocall.py
"""

import httpx
import json
import re
import sys

DOCTOR_BACKEND_URL = "https://doctor.chakravue.co.in"
CHANNEL_NAME = "test_diagnostic_channel"


def validate_agora_app_id(value: str) -> tuple:
    """Validate that a string is a valid 32-char hex Agora App ID."""
    if not value:
        return False, "EMPTY"
    trimmed = value.strip()
    if len(trimmed) != 32:
        return False, f"Wrong length: {len(trimmed)} (expected 32)"
    if not re.match(r"^[0-9a-fA-F]{32}$", trimmed):
        return False, f"Contains non-hex characters"
    return True, "VALID"


def main():
    print("=" * 60)
    print("  Video Call Token Diagnostic Test")
    print("=" * 60)
    print(f"\nDoctor Backend: {DOCTOR_BACKEND_URL}")
    print(f"Channel Name:   {CHANNEL_NAME}\n")

    try:
        with httpx.Client(timeout=15.0) as client:
            print(f"POST {DOCTOR_BACKEND_URL}/call/token?channel_name={CHANNEL_NAME}")
            resp = client.post(
                f"{DOCTOR_BACKEND_URL}/call/token",
                params={"channel_name": CHANNEL_NAME},
            )
            print(f"Status: {resp.status_code}")
            print(f"Headers: {dict(resp.headers)}\n")

            raw_text = resp.text
            print(f"Raw response body:\n{raw_text}\n")

            try:
                data = resp.json()
                print(f"Parsed JSON keys: {list(data.keys())}\n")

                # Check all known app_id field variants
                app_id_variants = [
                    "app_id", "appId", "appID", "APP_ID",
                    "agora_app_id", "agoraAppId", "agoraAppID",
                    "AGORA_APP_ID",
                ]
                print("Checking App ID field variants:")
                found_any = False
                for key in app_id_variants:
                    if key in data:
                        val = data[key]
                        valid, reason = validate_agora_app_id(str(val))
                        status = "OK" if valid else "FAIL"
                        print(f"  [{status}] '{key}' = '{val}' -> {reason}")
                        found_any = True

                if not found_any:
                    print("  [WARN] NONE of the known app_id field names found!")
                    print(f"  Available keys: {list(data.keys())}")
                    # Try case-insensitive search
                    for key in data.keys():
                        if "app" in key.lower() and "id" in key.lower():
                            val = data[key]
                            valid, reason = validate_agora_app_id(str(val))
                            status = "OK" if valid else "FAIL"
                            print(f"  [{status}] Found potential match: '{key}' = '{val}' -> {reason}")

                # Check token
                print("\nChecking token:")
                if "token" in data:
                    token = data["token"]
                    if token:
                        print(f"  [OK] token present (length={len(token)})")
                        print(f"  First 30 chars: {token[:30]}...")
                    else:
                        print(f"  [FAIL] token is empty/null")
                else:
                    print(f"  [FAIL] 'token' key not found in response")

            except json.JSONDecodeError:
                print("[FAIL] Response is NOT valid JSON!")

    except httpx.ConnectError as e:
        print(f"[FAIL] Connection failed: {e}")
    except Exception as e:
        print(f"[FAIL] Error: {e}")

    print("\n" + "=" * 60)
    print("  End of diagnostic test")
    print("=" * 60)


if __name__ == "__main__":
    main()
