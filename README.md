# Fluid Notifications

Glass notifications that grow out of the camera cut-out (Samsung / Android 12+).

## Get the APK with GitHub (no Android Studio needed)
1. Create a new repository on github.com (empty, no README).
2. Upload **the contents of this folder** (not the folder itself) so that `settings.gradle.kts` and
   `.github/workflows/build.yml` are at the top level of the repo.
   Tip: if the web uploader skips the hidden `.github` folder, use *Add file → Create new file*,
   type `.github/workflows/build.yml` as the name and paste the file contents.
3. Open the **Actions** tab → *Build APK* → wait ~3-5 minutes (it also starts automatically on every push;
   you can start it by hand with *Run workflow*).
4. Open the finished run → **Artifacts** → `FluidNotifications-apk` → download the zip → install `FluidNotifications.apk`.
   (Allow "install unknown apps" for your browser/file manager.)

For a permanent download link: create a tag like `v1.0` (Releases → *Draft a new release* → new tag `v1.0`);
the workflow attaches the APK to the release.

## First run
1. Grant: Notification access, Display over other apps, Battery → Unrestricted.
2. Samsung: Settings → Notifications → Notification pop-up style → Brief, to avoid double pop-ups.
3. Open **Test lab** and tap a scenario (use the delay to go to the home screen).
4. Tune everything in **Settings & live preview**.
