# KRAVEIT 1.3.1 Play candidate — versionCode 6

Production website confirmed by the owner on 2026-09-16: https://kraveit.netlify.app/.
Application ID remains ke.co.kraveit.app; target/compile SDK 36, minimum SDK 24.

This candidate adds supported AndroidX Back navigation, enabled only while WebView history exists. Root Back remains a system action. System bars, display cutouts and IME insets pad the content without accumulating padding; adjustResize supports older Android keyboards. VersionCode 6 distinguishes it from the previously built code 5; check Play Console for code availability before upload.

The source archive is checksum-verified before tracked overrides are applied. The workflow produces UNSIGNED AAB/APK files, source commit and checksums, plus release lint reports. Retention for new artifacts is 90 days. CI does not sign or publish.

## Evidence and limits

- Revised code-6 build: run 35556567644, commit 694b2a01ffbb2fdeb523ac6d579d36c16ad7d5d1. Compilation, release lint, AAB/APK assembly and artifact upload passed. No unit tests exist. Candidate remains unsigned; physical-device testing is pending.

- Original code-5 build: run 34922309875, commit 7a02d6f4f614ff0e4ab60d3c0129fb3ad2d6976e. Compilation/lint succeeded. Unit-test task was NO-SOURCE, not a test pass.
- Existing code-5 AAB located and verified: CMS signature, SHA-256 manifest digest and all 60 manifest-listed entry digests passed. AAB SHA-256: e12cd67e2e6a268bfb7bc6aa8f357da5d462f3dabf9e33c471efad707310f84f. This does not validate the revised code-6 candidate or prove Play enrollment.
- Existing stored V1.3.0 APK SHA-256: ff6c7b76a65d28be2b3c79fd9c9f4fde066f5e3131da253bcde3215606d2cff5.
- Its embedded JAR signer certificate SHA-256: 2C:89:A3:94:56:D4:E1:CA:28:CA:43:12:4C:2A:6A:43:C6:03:D0:6B:6C:20:AE:75:D6:9D:D4:78:2A:78:7D:1F. Extracted with keytool; full APK signature verification and comparison with the actually installed APK are still required.
- Original app-signing keystore and separate upload keystore located; both open successfully as PrivateKeyEntry. The original certificate matches the stored APK signer above. Upload certificate SHA-256: DB:27:26:FD:E5:E5:CA:50:42:27:C7:1F:30:E6:11:03:FE:C0:10:ED:8A:0B:98:2D:79:A4:18:FF:47:2C:4D:11, matching the existing AAB signer. No keys or passwords committed. Enrollment remains unverified.
- Domain choice is confirmed; current website behavior, privacy declarations, real-device results and Play Console state remain unverified.

## Release gates

1. Pass compilation and release lint for the revised commit; verify candidate checksums and source commit.
2. Run DEVICE_ACCEPTANCE.md on Android 13, 15, 16 and an older supported device.
3. Use the recovered original private app-signing key and separate upload key. Compare the distributed APK with the intended Play app-signing certificate. Do not substitute a fresh app-signing key if existing installs must update seamlessly.
4. Complete publisher verification; organization is the appropriate intended route for this commercial business, subject to the owner's final decision.
5. Configure Play App Signing, sign the new AAB with the registered upload key, upload to Internal testing and verify an update over the distributed APK without uninstalling.
6. Complete accurate store/privacy/Data safety/access/target audience/content rating/ads declarations, including the controlled website's behavior. Provide account deletion if registration is offered.
7. Complete any applicable closed-testing/production-access requirements and resolve pre-launch findings before production submission.

No merge or Play publication is authorized by this status document.
