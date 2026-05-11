# StopWatch v1.0.2 Task

Project at /home/jay/Documents/Scripts/AI/openclaw/Job8
GitHub: https://github.com/jnetai-clawbot/StopWatch

## Changes needed

### 1. Title centered
In app/src/main/res/layout/activity_main.xml, set the Toolbar's title gravity to center. The toolbar has `app:titleTextColor` — add `app:titleTextAlignment="center"` or `android:layout_gravity="center"`.

### 2. Settings as a tab in ViewPager
Add a 5th tab called "Settings" to the ViewPager2 (alongside Stopwatch, Timer, Alarm, About). Create a SettingsFragment (not Activity) that contains all the settings controls:
- Volume slider
- Sound selection with Preview button
- Upload MP3 button  
- Vibrate toggle
- Silent mode toggle
- Background toggle
- Save button (saves all settings - they already save on change, but add an explicit "Save Settings" button that shows a toast)

Files to edit:
- app/src/main/java/com/jnetai/stopwatch/fragments/adapters/ViewPagerAdapter.kt — add 5th tab, getItemCount = 5
- Create app/src/main/java/com/jnetai/stopwatch/fragments/SettingsFragment.kt
- Create app/src/main/res/layout/fragment_settings.xml (copy relevant settings controls from activity_settings.xml)
- app/src/main/java/com/jnetai/stopwatch/MainActivity.kt — add 4 -> getString(R.string.tab_settings) in TabLayoutMediator
- app/src/main/res/values/strings.xml — add <string name="tab_settings">Settings</string>
- Increment offscreenPageLimit to 4

Keep the existing SettingsActivity as well (the toolbar menu still opens it).

### 3. Check newest version in About tab
In AboutFragment.kt:
- Add a "Check for Updates" button that calls the GitHub API to get the latest release tag
- Compare against APP_VERSION
- Show a dialog/toast: "v1.0.2 is the latest version" or "v1.0.3 available! Download at..."
- Use HttpURLConnection to fetch https://api.github.com/repos/jnetai-clawbot/StopWatch/releases/latest
- Parse the JSON response's "tag_name" field
- Run on a background thread (AsyncTask or coroutine — use Thread { ... }.start() for simplicity)
- Add INTERNET permission (already in AndroidManifest.xml)

In fragment_about.xml:
- Replace the "View Releases" button with a "Check for Updates" button
- Or add both

### 4. Sound preview with explicit button
In SettingsFragment:
- When user selects a sound from the picker dialog, auto-preview it (already done in SettingsActivity)
- Also add a "Preview Sound" button next to the current sound display that play the current sound at 30% volume

### 5. Bump version
- app/build.gradle: versionCode 3, versionName "1.0.2"
- AboutFragment.kt companion: APP_VERSION = "1.0.2"

### 6. Commit, push, tag v1.0.2
- Delete old v1.0.1 tag first if needed
- git add -A
- git commit -m "v1.0.2: Settings tab, version check, title centered, sound preview"
- git push origin main
- git tag v1.0.2
- git push origin v1.0.2

### 7. Wait for GitHub Actions build
- Poll GitHub API every 30 seconds until build appears and completes
- Check conclusion == "success"

### 8. Signal ready
When build succeeds, write "READY_TO_TEST" to status.flag

## Important notes
- Don't edit PROMPT.md
- Don't edit Backup/ folder
- The staged changes from earlier (stopwatch fix, countdown restore fix) are already staged — include them in the commit (git add -A will pick them up)
- Update changes.txt with all changes
