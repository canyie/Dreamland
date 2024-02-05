# Dreamland [![Build Status](https://dev.azure.com/ssz33334930121/ssz3333493/_apis/build/status/canyie.Dreamland?branchName=master)](https://dev.azure.com/ssz33334930121/ssz3333493/_build/latest?definitionId=1&branchName=master)

[中文版本](https://github.com/canyie/Dreamland/blob/master/README_CN.md)

## Introduction
Dreamland is a Xposed framework implementation, you can use it to use Xposed modules.
- Supports Android 5.0~**14**
- Low invasiveness
- For hooking third apps, only requires restart the target app, not the device
- Strictly restrict modules, you can choose which apps need to load which modules

**Note: Currently I do not have enough time and energy to maintain this project, so the development of this project will be inactive (but will not stop). I think other mature projects (like [LSPosed](https://github.com/LSPosed/LSPosed) and [TaiChi](https://taichi.cool/) ) will be good substitutes.**

## Warning
**Dreamland needs to modify the system, installing Dreamland is dangerous; In any case, please back up your data yourself and be prepared for equipment damage, your use of Dreamland is at your own risk, we are not responsible for any of your losses.**

## Install
1. Install Magisk (the latest version is recommended).
2. Install [Riru](https://github.com/RikkaApps/Riru) or turn on Zygisk in Magisk app and reboot.
3. Download and install Dreamland in Magisk app. Installing from custom recovery is NO longer supported.
4. Install [Dreamland Manager](https://github.com/canyie/DreamlandManager/releases)
5. Reboot. (For Magisk < 21006, you have to reboot twice.)

## Download channel
- Beta: Test version, released by the developer, download from our [GitHub Release Page](https://github.com/canyie/Dreamland/releases).
- Canary: Test version, automatically build by CI, use of the version is at your own risk. Download from [Azure Pipelines](https://dev.azure.com/ssz33334930121/ssz3333493/_build/latest?definitionId=1&branchName=master).

## Known issues
- Thanox not working. Do NOT use Thanox otherwise your device will go into bootloop.
- [New XSharedPreferences](https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences) is not implemented in Dreamland, some modules that requires this feature will not work. We are planing a new API named "XRemotePreferences", please go to [this page](https://github.com/libxposed/XposedService/issues/1) to learn more and talk to us.

## Discussion
- [QQ Group: 949888394](https://shang.qq.com/wpa/qunwpa?idkey=25549719b948d2aaeb9e579955e39d71768111844b370fcb824d43b9b20e1c04)
- [Telegram Group: @DreamlandFramework](https://t.me/DreamlandFramework)

## Credits
- [Xposed](https://github.com/rovo89/Xposed): the original Xposed framework
- [EdXposed](https://github.com/ElderDrivers/EdXposed)
- [LSPosed](https://github.com/LSPosed/LSPosed): another modern Xposed framework
- [Magisk](https://github.com/topjohnwu/Magisk): makes all these possible
- [Riru](https://github.com/RikkaApps/Riru): provides a way to inject codes into zygote process
- [Pine](https://github.com/canyie/pine): ART hooking library
