# macOS Distributable (DMG)

Prerequisites:
- macOS with Xcode Command Line Tools installed (provides `codesign`, `notarytool`, `stapler`).
- JDK 25 installed and `JAVA_HOME` pointing to it (jpackage is included with the JDK).
- Maven 3.8+.

Build steps (local/dev, unsigned):
1. From the project root, run:
   mvn -Pmac clean package
2. The DMG will be created under:
   target/dist/family-tree-editor-${project.version}-macos*.dmg

About Gatekeeper warning:
- If you try to open an unsigned/unnotarized app, macOS may say "Apple cannot check [app] for malicious software" or "Apple cannot verify … is free of malware".
- For development builds you can bypass the warning by Control-clicking the app in Finder and choosing "Open" once, or by allowing it in System Settings → Privacy & Security.
- For distribution to end users, you must code sign with a Developer ID certificate and notarize with Apple.

Signed builds with jpackage (manual):
- The POM supports optional signing when you pass these properties:
  - -Dmac.sign=true
  - -Dmac.signing.user="Developer ID Application: Your Name (TEAMID)"
  - -Dmac.package.identifier=com.familytree.editor
  - Optionally: -Dmac.entitlements=/absolute/path/to/entitlements.plist
- Example:
  mvn -Pmac -Dmac.sign=true -Dmac.signing.user="Developer ID Application: John Appleseed (ABC1234567)" clean package

Notarization (manual):
1. After a signed DMG is built, submit to Apple:
   xcrun notarytool submit target/dist/*.dmg --key-id <KEY_ID> --issuer <ISSUER_ID> --key <path-to-api-key.p8> --wait
2. Staple the ticket to the DMG:
   xcrun stapler staple target/dist/*.dmg
3. Verify:
   xcrun stapler validate target/dist/*.dmg

CI/CD (GitHub Actions):
- The release workflow can sign and notarize macOS artifacts automatically when the following repository secrets are configured:
  - MAC_SIGNING_IDENTITY: Developer ID Application identity string.
  - MAC_CERT_BASE64: Base64-encoded .p12 (Developer ID Application) certificate.
  - MAC_CERT_PASSWORD: Password for the .p12.
  - NOTARY_API_KEY_ID: App Store Connect API key ID.
  - NOTARY_API_KEY_ISSUER: App Store Connect issuer ID.
  - NOTARY_API_KEY_P8: Contents of the .p8 private key (paste secret text).
- When these are present, the macOS jobs will build with -Dmac.sign=true, then notarize and staple the DMG automatically.

Notes:
- The build profile uses jpackage to create a custom runtime image with the required JavaFX modules for a self-contained app bundle.
- The macOS DMG requires a numeric app version (major.minor.patch). The build extracts this from the project version and ignores any qualifier like -SNAPSHOT.
- macOS installer tools do not accept versions starting with 0. If your project version begins with 0, the build automatically uses 1.minor.patch for the DMG.
