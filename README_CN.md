# 梦境框架 [![Build Status](https://dev.azure.com/ssz33334930121/ssz3333493/_apis/build/status/canyie.Dreamland?branchName=master)](https://dev.azure.com/ssz33334930121/ssz3333493/_build/latest?definitionId=1&branchName=master)

[English](README.md)

## 简介
梦境是另一种Xposed框架实现，你可以借助她直接使用Xposed模块。
- 支持 Android 7.0~**11**
- 低侵入性
- 对于hook非系统应用（及部分系统应用），只需要目标应用重启而非整个设备
- 严格限制模块，你可以选择哪些应用需要加载哪些模块

**注: 目前我没有足够的时间和精力去维护这个项目, 此项目接下来的开发将不活跃（但不会停止）。其他成熟的框架实习（如 [LSPosed](https://github.com/LSPosed/LSPosed) 和 [太极](https://taichi.cool/)）会是好的替代品。

## 警告
**安装梦境是危险的; 无论如何，请自己备份好数据并做好设备完全损坏的准备, 我们不对您的任何损失负责。**

## 安装
1. 安装 Magisk (推荐最新版本).
2. 从Magisk模块仓库安装 [Riru](https://github.com/RikkaApps/Riru)。注：我们建议您使较新版本的Riru，v21及更低版本容易被发现。
3. 下载 Dreamland ，然后在 Magisk Manager 或自定义 recovery 中刷入它。
4. 安装[Dreamland Manager](https://github.com/canyie/DreamlandManager/releases)。
5. 重启. (对于 Magisk < 21006, 你需要重启至少两次。)

## 下载通道
- Beta: 测试版本，由开发者发布。从我们的 [GitHub Release](https://github.com/canyie/Dreamland/releases) 中下载。
- Canary: 测试版本，由CI自动构建，使用风险自负。可以在 [Azure Pipelines](https://dev.azure.com/ssz33334930121/ssz3333493/_build/latest?definitionId=1&branchName=master) 下载到.

## 已知问题
- Thanox 模块不工作。请勿使用 Thanox 模块否则你的设备会开不了机。
- [New XSharedPreferences](https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences) 未实现在梦境中，需要该功能的模块不会工作。我们正在计划一个叫做 "XRemotePreferences" 的新 API，它稳定、实现漂亮、易于维护，请至[此页面](https://github.com/libxposed/XposedService/issues/1) 了解更多并与我们交流。

## 交流
- [QQ群: 949888394](https://shang.qq.com/wpa/qunwpa?idkey=25549719b948d2aaeb9e579955e39d71768111844b370fcb824d43b9b20e1c04)
- [Telegarm群：@DreamlandFramework](https://t.me/DreamlandFramework)

## 鸣谢
- [Xposed](https://github.com/rovo89/Xposed): 原版Xposed框架
- [LSPosed](https://github.com/LSPosed/LSPosed): 另一个现代化的Xposed框架
- [Magisk](https://github.com/topjohnwu/Magisk/): 让这一切成为可能
- [Riru](https://github.com/RikkaApps/Riru): 提供一种方法注入zygote进程
- [Pine](https://github.com/canyie/pine): ART hook库
