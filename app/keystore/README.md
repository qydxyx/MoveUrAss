# Release Keystore

This directory contains the permanent default signing keystore for MoveUrAss releases to ensure consistent signing keys across builds and prevent installation conflicts between versions:

- **File**: `release.jks`
- **Alias**: `moveurass`
- **Store Password**: `moveurass`
- **Key Password**: `moveurass`
- **SHA-256 Fingerprint**: `94:81:A5:2A:B7:E8:A7:82:F4:DB:6A:66:E7:71:4A:7E:69:6A:AE:17:AD:17:E5:25:11:C5:31:A1:2E:DE:12:65`

If custom release signing credentials are desired in CI, configure the `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` repository secrets in GitHub Actions.
