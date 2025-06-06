# Usb Backup - Auto-Backup your files to removable Storage!
[![license: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://github.com/newhinton/UsbBackup/blob/master/LICENSE) [![Latest Downloads](https://img.shields.io/github/downloads/newhinton/usbbackup/latest/total
)](https://github.com/newhinton/UsbBackup/releases) [![GitHub release](https://img.shields.io/github/v/release/newhinton/usbbackup?include_prereleases)](https://github.com/newhinton/UsbBackup/releases/latest) [![F-Droid](https://img.shields.io/f-droid/v/de.felixnuesse.usbbackup?logo=fdroid&logoColor=white)](https://f-droid.org/packages/de.felixnuesse.usbbackup/) [![IzzyOnDroid](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/de.felixnuesse.usbbackup&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAMAAABg3Am1AAAA4VBMVEXn9cuv7wDB9iGp4x2k5gKh3B6k3SyAxAGd4ASo6gCv5SCW2gHA7UTB6V+EwiOw3lK36zC+422d1yO78SWs3kfR7JhQiw2751G7+QCz8gCKzgGq3zay5DSm2jrF9jZLfwmNyiC77zXO7oaYzjW37CLj9Lze8LLA43uz3mK19ACR1QBcnRO78R6ExBek1kbE8FLI6nSPu0jH5YJxtQ2b1RiAmz53uwF7pitZkAeX1w7I72TY8KTO8HXD7La+0pKizWBzhExqjytpmR+UzSTA5Ctzy3uv1nOv3gyF3UuCsDRHcEx7M2pHAAAAS3RSTlP//////////////////////////////////////////////////////////////////////////////////////////////////wDLGfCsAAAB9ElEQVRIx72W53biMBCFhY0L7g0bTAktQEwgdMhuerbO+z/Q2sBiY0uKcvacnX8a3Y/R8YyuQPDJQP8KoExcro6ZC6C4TQXQx/oLABV3cfozgBgL/AWY9ScAsR7oBCD2AmSAoD8A+J3cWYECdBEaVm2z+U1hAuDx4fr6a08PGuuf6cmys5QvMEz0c12zhPWaAYBq9emp9/DlTrMUXsBOaw5Yjl5elrG+u9tYAxbAtjeL+Z3Wdl83Ovfr3BQyYAZBoLXbHDfQ2hykTSEAAIu+2LRcl4tD6UCm67jPCvD4/ON5YRhGpzOdrlar74fT5IcvOxDD0Xg0nvU7hjGVttv+0vYyAgyQdNgeey3Hce5DSZqN9GZmvzh8UO0F3thsiY4gqGoUtuL2AeaKpom5brVMryEKvCyXZVX0urd0wOxy4qwh8jxfLlcqZafpYoH0MzQGnNI/6CulOASFc/NWlZ17ADEG3oWjvn5TEvjbfJuyrnFaSfdyrK/f1Gp1tTAHF750aqgUJUCsr5UizFUv3EeQwmOFekmVmABDCiNVlqNwOwEqcM75vp+s/asrKpAmdxM/Gbnfuz0j8OYnPw2v9AqZ5Nt+f7hikwkw2T3Fc2l2jzdcst3DpwGCnvQ+EPUEu8c/STSAqMfZPeX5IQK0J+a//zn5MP4Am7ISN/4mSV8AAAAASUVORK5CYII=)](https://apt.izzysoft.de/packages/de.felixnuesse.usbbackup)
![supportive flags](https://img.shields.io/badge/support-🇺🇦_🏳️‍⚧_🏳️‍🌈-4aad4e)
[![Android Lint](https://github.com/newhinton/UsbBackup/actions/workflows/lint.yml/badge.svg)](https://github.com/newhinton/UsbBackup/actions/workflows/lint.yml)

A small app that allows you to quickly back up important files to thumb-drives!


## Screenshots
<table>
  <tr style="border:none">
    <td style="border:none">
      <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="360vh" />
    </td>
    <td style="border:none">
      <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="360vh" />
    </td>
  </tr>
</table>


## Features

- **Automation** Automatically start backups! 
- **Integration** Use any Cloud Provider on your phone as a source!
- **Material 3 Design** (Dark theme)
- **AES Encryption** Encrypt your files on your drive. WARNING: ENCRYPTION CODE NOT AUDITED
- **PC Decryption Tool** When backing up, we create a decryption tool that you can use on any pc to open your backups! 


## Installation

Grab the [latest version](https://github.com/newhinton/UsbBackup/releases/latest) of the signed APK and install it on your phone.

[<img src="https://f-droid.org/badge/get-it-on.png"
alt="Get it on F-Droid"
height="80">](https://f-droid.org/packages/de.felixnuesse.usbbackup)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png"
alt="Get it on IzzyOnDroid"
height="80">](https://apt.izzysoft.de/packages/de.felixnuesse.usbbackup)

## Usage


## Libraries
- [rclone](https://github.com/rclone/rclone) - Calling this a library is an understatement. Without rclone, there would not be Round Sync. See https://rclone.org/donate/ to support rclone.
- [Jetpack AndroidX](https://developer.android.com/license)
- [Floating Action Button SpeedDial](https://github.com/leinardi/FloatingActionButtonSpeedDial) - A Floating Action Button Speed Dial implementation for Android that follows the Material Design specification.
- [Glide](https://github.com/bumptech/glide) - An image loading and caching library for Android focused on smooth scrolling.
- [MarkdownJ](https://github.com/myabc/markdownj) - converts markdown into HTML.
- [Material Design Icons](https://github.com/Templarian/MaterialDesign) - 2200+ Material Design Icons from the Community.
- [Recyclerview Animators](https://github.com/wasabeef/recyclerview-animators) - An Android Animation library which easily add itemanimator to RecyclerView items.
- [Toasty](https://github.com/GrenderG/Toasty) - The usual Toast, but with steroids.
- Icons from [Flaticon](https://www.flaticon.com) courtesy of [Smashicons](https://www.flaticon.com/authors/smashicons) and [Freepik](https://www.flaticon.com/authors/freepik)


## Contributing
See [CONTRIBUTING](./CONTRIBUTING.md)

Anyone is welcome to contribute and help out. However, hate, discrimination and racism are decidedly unwelcome here. If you feel offended by this, you might belong to the group of people who are not welcome. I will not tolerate hate in any way.


## Developing

You should first make sure you have:
- Java installed and in your PATH
- Android SDK command-line tools installed

You can then build the app normally from Android Studio or from CLI by running:

```sh
# Debug build
./gradlew assembleDebug

# or release build
./gradlew assembleRelease
```

## Todo:

- Passwords in AddView
- Readme in assets for extraction tool
- EditView
- Option to switch between usb-detection modes
- file support
- folder/filepicker selector popup
- in-app-decryption of files

## License
This app is released under the terms of the [GPLv3 license](https://github.com/newhinton/usbbackup/blob/master/LICENSE).