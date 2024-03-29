SIMPLIFIED_CHINESE=false
if [ "$BOOTMODE" = "true" ]; then
  locale=$(getprop persist.sys.locale|awk -F "-" '{print $1"_"$NF}')
  [ "$locale" = "" ] && locale=$(settings get system system_locales|awk -F "," '{print $1}'|awk -F "-" '{print $1"_"$NF}')
  [ "$locale" = "zh_CN" ] && SIMPLIFIED_CHINESE=true
fi

if [ "$SIMPLIFIED_CHINESE" = "true" ]; then
  ALERT_ARCH="设备架构："
  ALERT_ANDROID_API="Android API 级别："
  ALERT_RIRU_API="Riru API 版本："
  ALERT_BOOTMODE="设备已启动到 Android 系统"
  ALERT_DETECTING_FLAVOR="正在检测安装环境"
  ALERT_ZYGISK_FLAVOR="Zygisk 已启用，将安装为 Zygisk 版本"
  ALERT_RIRU_FLAVOR="Zygisk 未启用，将安装为 Riru 版本"
  ALERT_EXTRACT_MODULE_FILES="正在解压模块文件"
  ALERT_REMOVE_LIB64="正在移除 64 位支持库"
  ALERT_FLAVOR_SPECIFC="开始 flavor 特定安装"
  ALERT_PREPARE_LOCAL_DIR="正在准备配置目录"
  ALRET_REMOVE_SEPOLICY_1="Magisk 版本低于 20.2，正在移除 sepolicy.rule"
  ALRET_REMOVE_SEPOLICY_2="我们建议您升级 Magisk"
  ALERT_REBOOT_TWICE_1="Magisk 版本低于 21006"
  ALERT_REBOOT_TWICE_2="您可能需要重启设备两次才能工作"
  ALERT_SETTING_PERMISSIONS="正在设置文件权限"
  ALERT_OLD_RIRU="较旧的 Riru API 版本："
  ALERT_REMOVE_OLD_FOR_NEW_RIRU="Riru v25+，正在移除旧的模块路径"

  ERR_UNSUPPORTED_ARCH="不支持的设备架构："
  ERR_UNSUPPORTED_ANDROID_API="不支持的Android API级别："
  ERR_FLASH_FROM_RECOVERY_1="从 Recovery 刷入不再是受支持的安装方法"
  ERR_FLASH_FROM_RECOVERY_2="由于 Recovery 本身限制，它可能会引发一些奇怪问题"
  ERR_FLASH_FROM_RECOVERY_3="请从 Magisk app 安装模块而非 Recovery"
  ERR_ZYGISK_REQUIRES_24="Zygisk 版本只支持 Magisk v24+"
  ERR_NO_FLAVOR="请在 Magisk app 中打开 Zygisk 并重启或安装模块 'Riru'"
  ERR_UNSUPPORTED_RIRU_API="不支持的 Riru API版本："
  ERR_EXTRACT_MODULE_FILES="解压模块文件失败："
  ERR_EXTRACT_SYSTEM_FOLDER="解压system目录失败："
  ERR_FLAVOR_SPECIFC="无法进行 flavor 特定安装："
  ERR_COPY_PROP_TO_RIRU_MODULE_PATH="复制module.prop失败："
  ERR_PREPARE_LOCAL_DIR="无法创建配置目录："

  WARN_OLD_ANDROID_API="未测试，可能不支持的 Android API "
  WARN_OLD_MANAGER_1="您安装了已弃用的旧版管理器"
  WARN_OLD_MANAGER_2="它与您正在安装的梦境框架不兼容"
  WARN_MANAGER_NOT_INSTALLED_1="梦境管理器未安装"
  WARN_MANAGER_NOT_INSTALLED_2="您将无法管理框架设置"
  WARN_PLEASE_INSTALL_NEW_MANAGER="请安装新版管理器！"
else
  ALERT_ARCH="Device architecture:"
  ALERT_ANDROID_API="Android API level:"
  ALERT_RIRU_API="Riru API version:"
  ALERT_BOOTMODE="Device is booted to Android system"
  ALERT_DETECTING_FLAVOR="Detecting installation flavor"
  ALERT_ZYGISK_FLAVOR="Zygisk is enabled, installing as Zygisk flavor"
  ALERT_RIRU_FLAVOR="Zygisk not enabled, installing as Riru flavor"
  ALERT_EXTRACT_MODULE_FILES="Extracting module files"
  ALERT_REMOVE_LIB64="Removing 64-bit libraries"
  ALERT_FLAVOR_SPECIFC="Starting flavor specific installations"
  ALERT_PREPARE_LOCAL_DIR="Preparing local directory"
  ALRET_REMOVE_SEPOLICY_1="Removing sepolicy because of your Magisk version is lower than 20.2"
  ALRET_REMOVE_SEPOLICY_2="We recommend that you upgrade Magisk to the latest version"
  ALERT_REBOOT_TWICE_1="Magisk version below 21006"
  ALERT_REBOOT_TWICE_2="You may need to manually reboot twice"
  ALERT_SETTING_PERMISSIONS="Setting permissions"
  ALERT_OLD_RIRU="Old Riru API version: "
  ALERT_REMOVE_OLD_FOR_NEW_RIRU="Removing old module path for Riru v25+"

  ERR_UNSUPPORTED_ARCH="Unsupported architecture:"
  ERR_UNSUPPORTED_ANDROID_API="Unsupported Android API level"
  ERR_FLASH_FROM_RECOVERY_1="NO longer support installing from Recovery"
  ERR_FLASH_FROM_RECOVERY_2="Recovery sucks, and installing from it may cause weird problems"
  ERR_FLASH_FROM_RECOVERY_3="Please install module from Magisk app instead of Recovery"
  ERR_ZYGISK_REQUIRES_24="Zygisk flavor requires Magisk v24+"
  ERR_NO_FLAVOR="Please turn on Zygisk from Magisk app and reboot, or install module 'Riru'"
  ERR_UNSUPPORTED_RIRU_API="Unsupported Riru API version"
  ERR_EXTRACT_MODULE_FILES="Can't extract module files:"
  ERR_EXTRACT_SYSTEM_FOLDER="Can't extract the system folder:"
  ERR_FLAVOR_SPECIFC="Unable to do flavor specific installation"
  ERR_COPY_PROP_TO_RIRU_MODULE_PATH="Can't copy module.prop to the riru module path:"
  ERR_PREPARE_LOCAL_DIR="Can't create the local directory:"

  WARN_OLD_ANDROID_API="Dreamland framework is not completely tested with your Android API "
  WARN_OLD_MANAGER_1="Detected deprecated dreamland manager"
  WARN_OLD_MANAGER_2="It is not compatible with current framework version"
  WARN_MANAGER_NOT_INSTALLED_1="Dreamland Manager not found"
  WARN_MANAGER_NOT_INSTALLED_2="You cannot manage Dreamland configurations"
  WARN_PLEASE_INSTALL_NEW_MANAGER="Please install new Dreamland Manager at https://github.com/canyie/DreamlandManager/releases"
fi
