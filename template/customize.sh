SKIPUNZIP=1

RIRU_OLD_PATH=/data/misc/riru
RIRU_OLD_MODULE_PATH=$RIRU_OLD_PATH/modules/dreamland
RIRU_NEW_PATH=/data/adb/riru
RIRU_NEW_MODULE_PATH=$RIRU_NEW_PATH/modules/dreamland
DREAMLAND_PATH=/data/misc/dreamland

ui_print "- Loading languages"
unzip -o "$ZIPFILE" languages.sh -d "$TMPDIR" >&2
[ -f "$TMPDIR/languages.sh" ] || abort "! Unable to extract languages.sh"
. "$TMPDIR/languages.sh"

if [ "$ARCH" != "arm64" ] && [ "$ARCH" != "arm" ]; then
  abort "! $ERR_UNSUPPORTED_ARCH $ARCH"
else
  ui_print "- $ALERT_ARCH $ARCH"
fi

if [ "$API" -lt 24 ]; then
  abort "! $ERR_UNSUPPORTED_ANDROID_API $API"
else
  ui_print "- $ALERT_ANDROID_API $API"
fi

if [ -f "$RIRU_OLD_PATH/api_version.new" ] || [ -f "$RIRU_OLD_PATH/api_version" ]; then
  RIRU_PATH="$RIRU_OLD_PATH"
  RIRU_MODULE_PATH="$RIRU_OLD_MODULE_PATH"
elif [ -f "$RIRU_NEW_PATH/api_version.new" ] || [ -f "$RIRU_NEW_PATH/api_version" ]; then
  RIRU_PATH="$RIRU_NEW_PATH"
  RIRU_MODULE_PATH="$RIRU_NEW_MODULE_PATH"
else
  abort "! $ERR_RIRU_NOT_INSTALLED"
fi

RIRU_API_VERSION=$(cat "$RIRU_PATH/api_version.new") || RIRU_API_VERSION=$(cat "$RIRU_PATH/api_version") || RIRU_API_VERSION=0
ui_print "- $ALERT_RIRU_API $RIRU_API_VERSION"

RIRU_MIN_API=$(grep_prop api "$TMPDIR/module.prop")
[ "$RIRU_API_VERSION" -ge "$RIRU_MIN_API" ] || abort "! $ERR_UNSUPPORTED_RIRU_API $RIRU_API_VERSION"

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
unzip -o "$ZIPFILE" 'system/*' -d "$MODPATH" >&2 || abort "! $ERR_EXTRACT_SYSTEM_FOLDER $?"

if [ "$IS64BIT" = "false" ]; then
  ui_print "- $ALERT_REMOVE_LIB64"
  rm -rf "$MODPATH/system/lib64"
fi

ui_print "- $ALERT_EXTRACT_RIRU_FILES"
[ -d "$RIRU_MODULE_PATH" ] || mkdir -p "$RIRU_MODULE_PATH" || abort "! $ERR_CREATE_RIRU_MODULE_PATH $?"
cp -f "$TMPDIR/module.prop" "$RIRU_MODULE_PATH/module.prop" || abort "! $ERR_COPY_PROP_TO_RIRU_MODULE_PATH $?"

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
