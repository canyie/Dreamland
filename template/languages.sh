SIMPLIFIED_CHINESE=false
if [ "$BOOTMODE" = "true" ]; then
  locale=$(getprop persist.sys.locale|awk -F "-" '{print $1"_"$NF}')
  [ "$locale" = "" ] && locale=$(settings get system system_locales|awk -F "," '{print $1}'|awk -F "-" '{print $1"_"$NF}')
  [ "$locale" = "zh_CN" ] && SIMPLIFIED_CHINESE=true
fi

if [ "$SIMPLIFIED_CHINESE" = "true" ]; then
  ALERT_ARCH="设备架构："
  ALERT_ANDROID_API="Android API级别："
  ALERT_RIRU_API="Riru API版本："
  ALERT_EXTRACT_MODULE_FILES="正在解压模块文件"
  ALERT_REMOVE_LIB64="正在移除64位支持库"
  ALERT_EXTRACT_RIRU_FILES="正在初始化Riru模块文件"
  ALERT_PREPARE_LOCAL_DIR="正在准备配置目录"
  ALRET_REMOVE_SEPOLICY_1="Magisk版本低于20.2，正在移除sepolicy.rule"
  ALRET_REMOVE_SEPOLICY_2="我们建议您升级Magisk"
  ALERT_REBOOT_TWICE_1="Magisk版本低于21006"
  ALERT_REBOOT_TWICE_2="您可能需要重启设备两次才能工作"
  ALERT_SETTING_PERMISSIONS="正在设置文件权限"
  ALERT_OLD_RIRU="较旧的Riru API版本："
  ALERT_REMOVE_OLD_FOR_NEW_RIRU="Riru v25+，正在移除旧的模块路径"

  ERR_UNSUPPORTED_ARCH="不支持的设备架构："
  ERR_UNSUPPORTED_ANDROID_API="不支持的Android API级别："
  ERR_RIRU_NOT_INSTALLED="请先安装模块 'Riru'"
  ERR_UNSUPPORTED_RIRU_API="不支持的 Riru API版本："
  ERR_EXTRACT_MODULE_FILES="解压模块文件失败："
  ERR_EXTRACT_SYSTEM_FOLDER="解压system目录失败："
  ERR_CREATE_RIRU_MODULE_PATH="创建riru模块路径失败："
  ERR_COPY_PROP_TO_RIRU_MODULE_PATH="复制module.prop失败："
  ERR_PREPARE_LOCAL_DIR="无法创建配置目录："

  WARN_OLD_MANAGER_1="您安装了已弃用的旧版管理器"
  WARN_OLD_MANAGER_2="它与您正在安装的梦境框架不兼容"
  WARN_MANAGER_NOT_INSTALLED_1="梦境管理器未安装"
  WARN_MANAGER_NOT_INSTALLED_2="您将无法管理框架设置"
  WARN_PLEASE_INSTALL_NEW_MANAGER="请安装新版管理器！"
else
  ALERT_ARCH="Device architecture:"
  ALERT_ANDROID_API="Android API level:"
  ALERT_RIRU_API="Riru API version:"
  ALERT_EXTRACT_MODULE_FILES="Extracting module files"
  ALERT_REMOVE_LIB64="Removing 64-bit libraries"
  ALERT_EXTRACT_RIRU_FILES="Extracting riru files"
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
  ERR_RIRU_NOT_INSTALLED="Requirement module 'Riru' is not installed"
  ERR_UNSUPPORTED_RIRU_API="Unsupported Riru API version"
  ERR_EXTRACT_MODULE_FILES="Can't extract module files:"
  ERR_EXTRACT_SYSTEM_FOLDER="Can't extract the system folder:"
  ERR_CREATE_RIRU_MODULE_PATH="Can't create riru module path:"
  ERR_COPY_PROP_TO_RIRU_MODULE_PATH="Can't copy module.prop to the riru module path:"
  ERR_PREPARE_LOCAL_DIR="Can't create the local directory:"

  WARN_OLD_MANAGER_1="Detected deprecated dreamland manager"
  WARN_OLD_MANAGER_2="It is not compatible with current framework version"
  WARN_MANAGER_NOT_INSTALLED_1="Dreamland Manager not found"
  WARN_MANAGER_NOT_INSTALLED_2="You cannot manage Dreamland configurations"
  WARN_PLEASE_INSTALL_NEW_MANAGER="Please install new Dreamland Manager at https://github.com/canyie/DreamlandManager/releases"
fi

