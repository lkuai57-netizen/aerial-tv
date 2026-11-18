# MiTV focus candidate (owner-specific, not an official release)

Base: focus commit 291d9c48a1cbf9b8f92e785ce835fc6ea4ca047b, with the previously tested MiTV identity changes.
Build: `./gradlew -PmitvHome=true :app:testStandardReleaseUnitTest :app:assembleStandardRelease`.
Package: `com.ts.launcher`; component: `com.ts.launcher/app.orchard.tv.MainActivity`.
Version: 1.99-mitv-focus1 (148). Focus selection, tile order, appearance and remote navigation are preserved.
The account type/provider authority are isolated from the ordinary copy. Official update checks are disabled for this differently named copy.

The owner's MiTV-MTEQ0 / baymax / Android 10 / 22.6.27.2363 previously accepted this package as HOME, but separately launched Xiaomi BootActivity afterward. This APK alone does not prove or promise flicker-free boot. It never disables any system app.

## Install stage

The owner confirmed `pm path com.ts.launcher` returns no package. Install this APK as a separate application. Do not uninstall `app.orchard.tv`; its settings are retained there and are not automatically migrated. After installation, open the candidate explicitly:

```sh
am start -W -n com.ts.launcher/app.orchard.tv.MainActivity
```

Verify the UI and focus-position toggle. The existing Aerial guard may take over; this must be handled separately, retaining the original copy's data. No Xiaomi package is to be changed during this stage.

Before any later experiment disabling Xiaomi Home: verify actual HOME resolution to the candidate, physical Home operation, Settings/HDMI access, and a working ADB reconnect path; capture the current package/default state. Obtain the owner's informed agreement for the system change. Disabling Xiaomi Home can leave FallbackHome/black screen, and loss of ADB can prevent command-based recovery. OEM allowlist or firmware changes can invalidate a previously successful setup.

## Recovery

If a later explicitly approved experiment disabled Xiaomi Home, restore it FIRST while ADB still works:

```sh
pm enable --user 0 com.mitv.tvhome
cmd package set-home-activity --user 0 com.mitv.tvhome/.MainActivityUserMode
pm enable --user 0 app.orchard.tv
am start -W -n app.orchard.tv/.MainActivity
```

Verify a visible usable Home before disabling/removing the candidate. Do not clear data, change the existing voice/karaoke accessibility services, or disable Settings/cloud control.

## Signing and evidence

CI produces an intermediate build and verifies package/component/provider/dex metadata plus automated tests and lint comparison. The delivered APK is then re-signed privately using a retained owner-specific key; neither that key nor its password is in GitHub/CI artifacts. Future owner builds must use that same retained key with a higher version code. Do not install the intermediate CI debug-signed APK.

The final delivery includes signature/hash verification and an exact source commit. Automated checks are not television tests; boot/wake behavior must still be observed on this TV. The signed APK filename is `aerial-mitv-focus.apk`.
