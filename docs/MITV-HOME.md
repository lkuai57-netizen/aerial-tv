# Aerial MiTV Home test build

This is an experimental, separately installed Aerial build for one owner's Xiaomi ES43
(L43M7-ES), system model MiTV-MTEQ0, codename baymax, Android 10, build 22.6.27.2363.
It preserves Aerial's interface. It is not a ROM and does not unlock or flash the TV.

## Evidence and limits

The owner verified that the original app opens and its accessibility guard works,
but cold boot shows Aerial, then Xiaomi Home, then Aerial. Setting the original
app as Home reports Success while resolution still returns
`com.mitv.tvhome/.MainActivityUserMode`.
The package name `com.ts.launcher` is unoccupied on this TV.

CleanYourTV documents this package name in the Home allowlist on the same system build:
https://github.com/Gathin23/cleanyourtv/blob/main/docs/devices/xiaomi-mitv-mteq0/xiaomi-mitv-mteq0.md

That is evidence for a test, not proof that this Aerial build will eliminate the
cold-boot transition. The owner's current cloud allowlist has not been captured.
No actual-TV validation has yet been performed with this build.

## What changes

Build with `-PmitvHome=true`:
- application ID: `com.ts.launcher`; Kotlin namespace remains `app.orchard.tv`.
- version: `1.99-mitv1`, code 147.
- the sync authenticator and sync adapter use the new package's account type.
- upstream update checks are off; upstream APK installation is already rejected for renamed copies.
- the setup QR links here instead of giving commands for the original package.
- standard builds without the property retain their original identity and behavior.

The original Aerial is not overwritten. Its preferences and grants are not
automatically copied to this separate app. Do not uninstall it or clear its data.
The test package uses the CI runner's temporary debug signing key.
Future builds may have a different signature and cannot necessarily upgrade this
candidate in place. This is a trial, not a maintained release channel. No signing
private key is uploaded in the build artifacts.

## Build and verification

GitHub Actions runs the upstream standard release unit tests and lint, then the
same checks plus APK assembly with the MiTV property. Added identity tests cover
HOME resolution, component class names, provider/account isolation, and updates.
The artifact contains an APK, Android 9–11 dex metadata, checksums, manifest,
signature verification, and build commit. Test reports are separate artifacts.

## Staged TV test — do not run all steps blindly

Keep a working ADB connection. Commands below are inside an existing TV shell
unless marked as host commands. Do not disable any Xiaomi system application.

1. Confirm `pm path com.ts.launcher` is empty. Stop if another app owns the name.
2. Install the candidate as a separate app. For a host with ADB, use the matching
   files together:
   `adb install-multiple aerial-mitv-test1.apk aerial-mitv-test1.dm`.
   Android 10 uses the APK's embedded `28.dm`, extracted by the build workflow.
   If installed from a memory card, installation does not supply external dex metadata.
3. Open it explicitly:
   `am start -W -n com.ts.launcher/app.orchard.tv.MainActivity`.
   Verify the picture and remote navigation before continuing. The old guard
   may temporarily bring the original app back; this does not prove a crash.
4. After the candidate opens successfully, temporarily disable only the
   independently installed original Aerial to stop its guard competing:
   `pm disable-user --user 0 app.orchard.tv`.
   This retains its data. Do not disable `com.mitv.tvhome`.
5. Open the candidate again. Initially do not grant the candidate accessibility
   or notification revival permissions: this test must verify real Home ownership.
   Set:
   `cmd package set-home-activity --user 0 com.ts.launcher/app.orchard.tv.MainActivity`.
6. Read back:
   `cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`.
   Success requires `com.ts.launcher/app.orchard.tv.MainActivity`.
   A printed Success from set-home-activity alone is not sufficient.
7. If it still returns Xiaomi Home or another component, STOP and restore below.
   Do not disable Xiaomi Home to force the test.
8. Only after real Home ownership, verify the physical Home key, settings access,
   volume, installed app launch, and HDMI return. Then test standby/wake and one
   cold boot with the owner observing the screen.
   Record whether Xiaomi Home ever appears; foreground recovery alone is not a pass.

Do not run the upstream install/restore scripts for this package: they target
`app.orchard.tv`. Do not replace the entire accessibility service list; the
owner's voice and karaoke services must remain.

## Recovery to the pre-test setup

If the candidate fails or remains on Xiaomi Home, use the existing ADB connection:

```sh
pm enable --user 0 com.mitv.tvhome
cmd package set-home-activity --user 0 com.mitv.tvhome/.MainActivityUserMode
pm enable --user 0 app.orchard.tv
am start -W -n app.orchard.tv/.MainActivity
pm disable-user --user 0 com.ts.launcher
```

Then verify that the original Aerial guard returns and existing voice/karaoke
services remain. If its notification access was removed when disabled, restore
only its entry with:
`cmd notification allow_listener app.orchard.tv/app.orchard.tv.guard.RevivalListener`.
The original WRITE_SECURE_SETTINGS grant should remain; inspect before changing
anything. Aerial can restore its own accessibility service when opened.
No system partition, bootloader, firmware, or original app data is modified.

## 中文说明

这是保留原界面的独立测试版，不是国际版系统。自动检查通过也不等于
电视实测成功。先确认能打开，再暂时停用旧版 Aerial（保留数据），然后
验证系统真正认新包为默认桌面。不要停用小米系统桌面。
新旧版本的设置不自动共享，测试版也不接收官方自动更新。
