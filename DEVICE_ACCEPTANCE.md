# Device acceptance — KRAVEIT Play candidate

Record: tester/date, source commit, AAB hash, versionCode, install source, device model, Android version, Android System WebView version. Every row is PENDING until run. Attach a short recording or reproduction notes for failures.

| Scenario | Pass condition | Result |
|---|---|---|
| Android 13/15/16 + older supported device | Install/launch succeeds, no crash | PENDING |
| Gesture and three-button Back | Home → product → checkout → Back walks web history; at root system exits/backgrounds normally | PENDING |
| Restored history | After background/process recreation, Back still follows restored web history | PENDING |
| IME Back priority | First Back dismisses keyboard without leaving checkout; next Back navigates | PENDING |
| Bars/cutout/landscape | Header and native Home/Track/WhatsApp controls remain visible and tappable | PENDING |
| Keyboard open/close repeatedly | Checkout fields/buttons visible; no cumulative padding or blank space | PENDING |
| Cart/checkout | Correct items, extras, quantity, delivery fees and final total; order reaches staff | PENDING |
| Payment success/cancel/retry | Honest status; no duplicate order/charge on repeated tap or retry | PENDING |
| Tracking | Correct order and actual stages/estimates displayed | PENDING |
| Profile | Save, restart, restore and explicitly forget; forgotten details remain absent after restart | PENDING |
| Location | Denied/approximate/precise all behave correctly; manual checkout stays available | PENDING |
| Offline/recovery | Useful offline screen; recover via Home after network returns | PENDING |
| External links | WhatsApp/browser handoff and return work; absent handler causes no crash | PENDING |
| Bridge isolation | Foreign origins and subframes cannot read/save/clear native profile; unsupported WebView permits checkout without remembering | PENDING |
| Play-delivered upgrade | Install distributed APK, save profile; update through Internal testing without uninstalling; profile and ordering survive | PENDING |

The helper expects /checkout.html and DOM IDs name, phone, distance, notes, submitBtn. Validate against the current production website. Re-test after website changes even when the APK is unchanged.

Do not count Internal App Sharing as proof of production signing continuity: use the Internal testing track. Do not clear app data or uninstall during the upgrade acceptance case.
