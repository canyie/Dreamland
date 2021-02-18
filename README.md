# Dreamland [![Build Status](https://dev.azure.com/ssz33334930121/ssz3333493/_apis/build/status/canyie.Dreamland?branchName=master)](https://dev.azure.com/ssz33334930121/ssz3333493/_build/latest?definitionId=1&branchName=master)

[中文版本](README_CN.md)

## Introduction
Dreamland is a Xposed framework implementation, You can use it directly to use Xposed modules.
- Supports Android 7.0~**11**
- Low invasiveness
- For hooking third apps, only required restart target app, not reboot
- Strictly restrict modules, you can choose which applications you need to load modules

## Warning
**Dreamland needs to modify the system, installing Dreamland is dangerous; In any case, please back up your data yourself and be prepared for equipment damage, your use of Dreamland is at your own risk, we are not responsible for any of your losses.**

## Install
1. Install Magisk (the latest version is recommended).
2. Install [Riru](https://github.com/RikkaApps/Riru) from Magisk Repo.
3. Download and install Dreamland in Magisk Manager or custom recovery.
4. Install [Dreamland Manager](https://github.com/canyie/DreamlandManager/releases)
5. Reboot. (For Magisk < 21006, you have to reboot twice.)

## Download
- Alpha: Test version, released by the developer, download from our [GitHub Release Page](https://github.com/canyie/Dreamland/releases).
- Canary: Test version, automatically build by CI, use of the version is at your own risk. Download from [Azure Pipelines](https://dev.azure.com/ssz33334930121/ssz3333493/_build/latest?definitionId=1&branchName=master).

## Known issues
- Pending hook is not supported yet, some modules may not work properly. Known not working module: Bili Pudding.

## Discussion
- [QQ Group: 949888394](https://shang.qq.com/wpa/qunwpa?idkey=25549719b948d2aaeb9e579955e39d71768111844b370fcb824d43b9b20e1c04)
- [Telegram Group](https://t.me/DreamlandFramework)

## Credits
- [Xposed](https://github.com/rovo89/Xposed): the original Xposed framework
- [EdXposed](https://github.com/ElderDrivers/EdXposed): Elder driver Xposed Framework.
- [Magisk](https://github.com/topjohnwu/Magisk): makes all these possible
- [Riru](https://github.com/RikkaApps/Riru): provides a way to inject codes into zygote process
- [Pine](https://github.com/canyie/pine): ART hooking library 
- [xHook](https://github.com/iqiyi/xHook): GOT hook library
