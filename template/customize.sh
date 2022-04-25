SKIPUNZIP=1

RIRU_OLD_PATH=/data/misc/riru
RIRU_NEW_PATH=/data/adb/riru
RIRU_MODULE_ID=dreamland
DREAMLAND_PATH=/data/misc/dreamland
RIRU_API=0

ui_print "- Loading languages"
unzip -o "$ZIPFILE" languages.sh -d "$TMPDIR" >&2
[ -f "$TMPDIR/languages.sh" ] || abort "! Unable to extract languages.sh"
. "$TMPDIR/languages.sh"

if [ "$ARCH" != "arm64" ] && [ "$ARCH" != "arm" ]; then
  abort "! $ERR_UNSUPPORTED_ARCH $ARCH"
else
  ui_print "- $ALERT_ARCH $ARCH"
fi

if [ "$API" -lt 21 ]; then
  abort "! $ERR_UNSUPPORTED_ANDROID_API $API"
else
  ui_print "- $ALERT_ANDROID_API $API"
  [ "$API" -lt 24 ] && ui_print "- $WARN_OLD_ANDROID_API $API"
fi

if [ "$BOOTMODE" = "true" ]; then
  ui_print "- $ALERT_BOOTMODE"
else
  ui_print "! $ERR_FLASH_FROM_RECOVERY_1"
  ui_print "! $ERR_FLASH_FROM_RECOVERY_2"
  abort "! $ERR_FLASH_FROM_RECOVERY_3"
fi

MAGISK_TMP=$(magisk --path) || MAGISK_TMP="/sbin"

# "ZYGISK_ENABLED" is not an API but exported unexpectedly when installing from Magisk app
# Magisk doesn't provide an API to detect if Zygisk is working, so the only way is...
if [ "$ZYGISK_ENABLED" = "1" ] || [ -d "$MAGISK_TMP/.magisk/zygisk" ]; then
  [ "$MAGISK_VER_CODE" -lt 24000 ] && abort "! $ERR_ZYGISK_REQUIRES_24"
  FLAVOR="zygisk"
  ui_print "- $ALERT_ZYGISK_FLAVOR"
else
  ui_print "- $ALERT_RIRU_FLAVOR"
  MAGISK_CURRENT_RIRU_MODULE_PATH=$MAGISK_TMP/.magisk/modules/riru-core
  # Temporarily support for KernelSU
  [ "$KSU" = "true" ] && MAGISK_CURRENT_RIRU_MODULE_PATH="/data/adb/ksu/modules/riru-core"

  if [ -f $MAGISK_CURRENT_RIRU_MODULE_PATH/util_functions.sh ]; then
    # Riru V24+, api version is provided in util_functions.sh
    # I don't like this, but I can only follow this change
    RIRU_PATH=$MAGISK_CURRENT_RIRU_MODULE_PATH
    ui_print "- Load $MAGISK_CURRENT_RIRU_MODULE_PATH/util_functions.sh"
    # shellcheck disable=SC1090
    . $MAGISK_CURRENT_RIRU_MODULE_PATH/util_functions.sh

    # Pre Riru 25, as a old module
    if [ "$RIRU_API" -lt 25 ]; then
      ui_print "- Riru API version $RIRU_API is lower than v25"
      RIRU_PATH=$RIRU_NEW_PATH
    fi
  elif [ -f "$RIRU_OLD_PATH/api_version.new" ] || [ -f "$RIRU_OLD_PATH/api_version" ]; then
    RIRU_PATH="$RIRU_OLD_PATH"
  elif [ -f "$RIRU_NEW_PATH/api_version.new" ] || [ -f "$RIRU_NEW_PATH/api_version" ]; then
    RIRU_PATH="$RIRU_NEW_PATH"
  else
    abort "! $ERR_NO_FLAVOR"
  fi
  RIRU_MODULE_PATH="$RIRU_PATH/modules/$RIRU_MODULE_ID"

  [ "$RIRU_API" -ne 0 ] || RIRU_API=$(cat "$RIRU_PATH/api_version.new") || RIRU_API=$(cat "$RIRU_PATH/api_version")
  ui_print "- $ALERT_RIRU_API $RIRU_API"

  RIRU_MIN_API=$(grep_prop api "$TMPDIR/module.prop")
  [ "$RIRU_API" -ge "$RIRU_MIN_API" ] || abort "! $ERR_UNSUPPORTED_RIRU_API $RIRU_API"

  FLAVOR="riru"
fi

if [ "${BOOTMODE}" = "true" ]; then
  if [ "$(pm path 'top.canyie.dreamland.manager')" = "" ]; then
    if [ "$(pm path 'com.canyie.dreamland.manager')" != "" ]; then
      ui_print "- $WARN_OLD_MANAGER_1"
      ui_print "- $WARN_OLD_MANAGER_2"
    else
      ui_print "- $WARN_MANAGER_NOT_INSTALLED_1"
      ui_print "- $WARN_MANAGER_NOT_INSTALLED_2"
    fi
    ui_print "- $WARN_PLEASE_INSTALL_NEW_MANAGER"
  fi
fi

ui_print "- $ALERT_EXTRACT_MODULE_FILES"
unzip -o "$ZIPFILE" module.prop uninstall.sh post-fs-data.sh service.sh sepolicy.rule system.prop -d "$MODPATH" >&2 || abort "! $ERR_EXTRACT_MODULE_FILES $?"
unzip -o "$ZIPFILE" 'system/*' 'riru/*' -d "$MODPATH" >&2 || abort "! $ERR_EXTRACT_SYSTEM_FOLDER $?"

if [ "$IS64BIT" = "false" ]; then
  ui_print "- $ALERT_REMOVE_LIB64"
  rm -rf "$MODPATH/riru/lib64"
fi

ui_print "- $ALERT_FLAVOR_SPECIFC"
if [ "$FLAVOR" = "riru" ]; then
  if [ "$RIRU_API" -lt 25 ]; then
    ui_print "- $ALERT_OLD_RIRU $RIRU_API"
    mv -f "$MODPATH/riru/lib" "$MODPATH/system/"
    [ -d "$MODPATH/riru/lib64" ] && mv -f "$MODPATH/riru/lib64" "$MODPATH/system/" 2>&1
    rm -rf "$MODPATH/riru"
    [ -d $RIRU_MODULE_PATH ] || mkdir -p $RIRU_MODULE_PATH || abort "! Can't create $RIRU_MODULE_PATH: $?"
    cp -f "$MODPATH/module.prop" "$RIRU_MODULE_PATH/module.prop"
  else
    # Riru v25+, user may upgrade from old module without uninstall
    # Remove the Riru v22's module path to make sure riru knews we're a new module
    RIRU_22_MODULE_PATH="$RIRU_NEW_PATH/modules/$RIRU_MODULE_ID"
    ui_print "- $ALERT_REMOVE_OLD_FOR_NEW_RIRU"
    rm -rf "$RIRU_22_MODULE_PATH"
  fi
else
  mkdir -p "$MODPATH/zygisk"
  mv -f "$MODPATH/riru/lib/libriru_dreamland.so" "$MODPATH/zygisk/armeabi-v7a.so" || abort "! $ERR_FLAVOR_SPECIFC"
  [ -f "$MODPATH/riru/lib64/libriru_dreamland.so" ] && (mv -f "$MODPATH/riru/lib64/libriru_dreamland.so" "$MODPATH/zygisk/arm64-v8a.so" || abort "! $ERR_FLAVOR_SPECIFC")

  # Magisk won't load Riru modules if Zygisk enabled
  rm -rf "$MODPATH/riru" || abort "! $ERR_FLAVOR_SPECIFC"
fi

ui_print "- $ALERT_PREPARE_LOCAL_DIR"
[ -d "$DREAMLAND_PATH" ] || mkdir -p "$DREAMLAND_PATH" || abort "! $ERR_PREPARE_LOCAL_DIR $?"

if [ "$MAGISK_VER_CODE" -lt 20200 ]; then
  ui_print "- $ALRET_REMOVE_SEPOLICY_1"
  ui_print "- $ALRET_REMOVE_SEPOLICY_2"
  rm -f "$MODPATH/sepolicy.rule"
fi

# before Magisk 16e4c67, sepolicy.rule is copied on the second reboot
if [ "$MAGISK_VER_CODE" -lt 21006 ]; then
  ui_print "- $ALERT_REBOOT_TWICE_1"
  ui_print "- $ALERT_REBOOT_TWICE_2"
fi

ui_print "- $ALERT_SETTING_PERMISSIONS"
# The following is the default rule, DO NOT remove
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm_recursive "$DREAMLAND_PATH" 1000 1000 0700 0600 u:object_r:system_data_file:s0
