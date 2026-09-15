# KRAVEIT 1.3.1 Play candidate

This branch builds the existing reviewed V1.3 archive plus tracked overrides. It produces unsigned AAB/APK candidates without signing secrets. They must be signed before distribution; CI success is not Play approval.

Changes: increment version code to 5, preserve ke.co.kraveit.app and native profile storage, replace the unrestricted JavaScript interface with an origin-restricted message listener that rejects subframes, accept approximate location permission, and derive navigation URLs from the configured home URL. Unsupported WebViews retain checkout but do not expose native remembering. Existing saved profile fields are preserved.

The production host remains kraveit.netlify.app pending owner confirmation. The site, privacy declarations, Android 15/16 layout and Play-delivered upgrade path still need validation. Use the original app-signing certificate when enrolling in Play App Signing if existing direct APK installs must upgrade seamlessly; sign uploads with a separate upload key.

Workflow: KRAVEIT Play AAB candidate. A push to this branch or a manual run builds the unsigned candidates. Signed publishing is intentionally a separate step. No Play upload occurs in this workflow.
