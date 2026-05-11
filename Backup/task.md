# StopWatch App - Task List

Project at /home/jay/Documents/Scripts/AI/openclaw/Job8
GitHub: https://github.com/jnetai-clawbot/StopWatch
Branch: main

## Changes Required

### 1. Remove duplicate title bar
The top of the app shows "StopWatch" twice — once from the action bar/toolbar and once from somewhere else. Remove one. Keep only the toolbar title.

### 2. Fix stopwatch resume
In stopwatch mode, pressing Pause then Resume fails to resume. Fix the stopwatch fragment's pause/resume logic.

### 3. Add About as a 4th tab
Add an About tab to the ViewPager2 (alongside Stopwatch, Timer, Alarm). The tab should show:
- App name: StopWatch
- Version: v1.0.1 (must match the GitHub release tag)
- GitHub releases link (latest release, no direct filename)
- Share button that shares the GitHub releases URL
- Developer: JNetAI
- Website: jnetai.com with clickable link

### 4. Add silent alarm mode toggle to Settings
Add a "Silent Mode" toggle in Settings that when enabled, the alarm plays no sound but still vibrates (if vibration is also enabled). When silent mode is off, normal sound + vibration behavior applies.

### 5. Add Share App button to Settings/About
Button that shares text containing the GitHub releases URL (https://github.com/jnetai-clawbot/StopWatch/releases/latest)

### 6. Ensure background operation
Make sure all three modes (stopwatch, timer, alarm) continue running in background. The app should already have foreground service permissions. Ensure the timer/countdown doesn't stop when switching tabs or going to settings.

### What already exists (don't redo):
- Vibration toggle in Settings
- Volume slider in Settings
- Sound selection in Settings
- Background service toggle
- Keystore signing via GitHub Actions
- GitHub Actions workflow at .github/workflows/build.yml
- All sound files in sounds/

## Process
1. Edit files directly in the project directory
2. After all changes, commit with "Fix: [description]" and push to origin main
3. Tag v1.0.1 (delete old tag first, create new, push)
4. Wait for GitHub workflow to build
5. When workflow succeeds, write "READY_TO_TEST" to status.flag

## Important
- Don't edit PROMPT.md
- Don't edit Backup/ folder contents
- Always use same keystore (already configured)
- Save changes to changes.txt
