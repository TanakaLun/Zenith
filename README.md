<p align="center">
  <img src="fastlane/metadata/android/en-US/images/icon.png" width="128" height="128">
</p>

<h1 align="center">Zenith - A Material Design 3 Expressive Digital Wellbeing App</h1>

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/featureGraphic.png" alt="Feature Graphic" width="100%">
</p>

<p align="center">
  <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Android/android2.svg">&nbsp;&nbsp;
  <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/AndroidStudio/androidstudio2.svg">&nbsp;&nbsp;
  <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/Kotlin/kotlin2.svg">&nbsp;&nbsp;
  <img src="https://ziadoua.github.io/m3-Markdown-Badges/badges/LicenceGPLv3/licencegplv32.svg">
</p>

<p align="center">
  <strong>Zenith</strong> is a smart digital wellbeing assistant for Android, built with <strong>Material Design 3 Expressive</strong>. It uses proactive interventions and real-time monitoring to help you break addictive scrolling habits through a fluid, motion-rich experience.
</p>

<p align="center">
  <a href="https://github.com/1372Slash/Zenith/releases">
    <img src="https://img.shields.io/github/v/release/1372Slash/Zenith?label=Download%20Zenith&style=for-the-badge&color=6750A4&logo=android&logoColor=white">
  </a>
</p>

## Screenshots

<div align="center">
  <table border="0" cellpadding="0" cellspacing="2" style="border-collapse: collapse;">
    <tr style="border: none;">
      <td width="32%" style="border: none; padding: 2px;"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png"></td>
      <td width="32%" style="border: none; padding: 2px;"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png"></td>
      <td width="32%" style="border: none; padding: 2px;"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png"></td>
    </tr>
    <tr style="border: none;">
      <td width="32%" style="border: none; padding: 2px;"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png"></td>
      <td width="32%" style="border: none; padding: 2px;"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png"></td>
      <td width="32%" style="border: none; padding: 2px;"><img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png"></td>
    </tr>
  </table>
</div>

## Key Features

- **Bedtime Mode (New)**: Automate your digital detox with customizable schedules, Wind Down notifications, and automatic Do Not Disturb.
- **Interactive Widgets (New)**: Keep track of your app streaks and daily focus progress with Material 3 Expressive home screen widgets.
- **Mindful Gateway**: Proactively interrupt all non-whitelisted apps with a mindful pause to prevent mindless scrolling.
- **Backup & Restore**: Securely save your settings and schedules with automated periodic backups or manual exports.
- **Shield Mode**: Protect specific apps with mindful pauses and usage frequency limits.
- **Goal Pursuit**: Set and achieve target usage times for productive applications.
- **Session HUD**: A floating, customizable overlay (size & opacity) that shows remaining time for active sessions.
- **Early Kick**: Optional reminders or early ejection to help you transition out of apps before your limit expires.
- **Expressive Design**: Fully compliant with Material Design 3 Expressive guidelines, featuring fluid motion, adaptive typography (GSFlex), and floating navigation components.

## Installation Guide

1. Clone this repository.
2. Open it in Android Studio Ladybug (2024.2.1) or a newer version.
3. Ensure Android SDK 33+ is installed.
4. Build and run on a physical device (recommended for proper permission handling).

## Required Permissions

To function optimally, Zenith requires:
1. **Accessibility Service (Optional)**: To instantly detect foreground application changes and manage app interventions.
2. **Usage Access (PACKAGE_USAGE_STATS)**: To accurately calculate daily application usage statistics.
3. **Overlay Permission (SYSTEM_ALERT_WINDOW)**: To display Shield intervention screens and the Session HUD over other applications.
4. **Notification Permission & Listener (Optional)**: To provide focus updates and manage device notifications during Bedtime/Focus modes.
5. **Battery Optimization Exemption**: To ensure consistent background monitoring and prevent system-level termination.
6. **Exact Alarms**: To precisely trigger resets, bedtime schedules, and mindful reminders.

## Special Thanks

- **[Tomato](https://github.com/nsh07/Tomato)** - Interface and promotional material inspiration.

## License

This project is created for learning and self-development purposes.
[GNU GPL v3.0](LICENCE)
