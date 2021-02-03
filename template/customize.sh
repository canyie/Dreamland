RIRU_OLD_PATH=/data/misc/riru
RIRU_OLD_MODULE_PATH=$RIRU_OLD_PATH/modules/dreamland
RIRU_NEW_PATH=/data/adb/riru
RIRU_NEW_MODULE_PATH=$RIRU_NEW_PATH/modules/dreamland
DREAMLAND_PATH=/data/misc/dreamland

if [[ "$ARCH" != "arm64" && "$ARCH" != "arm" ]]; then
  abort "! Unsupported architecture: $ARCH"
else
  ui_print "- Device architecture: $ARCH"
fi

if [[ "$API" -lt 24 ]]; then
  abort "! Unsupported Android API level $API"
else
  ui_print "- Android API level: $API"
fi

if [[ -f "$RIRU_OLD_PATH/api_version.new" || -f "$RIRU_OLD_PATH/api_version" ]]; then
  RIRU_PATH="$RIRU_OLD_PATH"
  RIRU_MODULE_PATH="$RIRU_OLD_MODULE_PATH"
elif [[ -f "$RIRU_NEW_PATH/api_version.new" || -f "$RIRU_NEW_PATH/api_version" ]]; then
  RIRU_PATH="$RIRU_NEW_PATH"
  RIRU_MODULE_PATH="$RIRU_NEW_MODULE_PATH"
else
  abort "! Requirement module 'Riru - Core' is not installed"
fi

RIRU_API_VERSION=$(cat "$RIRU_PATH/api_version.new") || RIRU_API_VERSION=$(cat "$RIRU_PATH/api_version") || RIRU_API_VERSION=0
ui_print "- Riru API version: $RIRU_API_VERSION"

RIRU_MIN_API=$(grep_prop api "$TMPDIR/module.prop")
[[ "$RIRU_API_VERSION" -ge "$RIRU_MIN_API" ]] || abort "! Unsupported Riru API version $RIRU_API_VERSION"

if [[ "${BOOTMODE}" == "true" ]]; then
  if [[ $(pm path "top.canyie.dreamland.manager") == "" ]]; then
    if [[ $(pm path "top.canyie.dreamland.manager") != "" ]]; then
      ui_print "- Detected deprecated dreamland manager"
      ui_print "- It is not compatible with current framework version"
    else
      ui_print "- Dreamland Manager not found"
      ui_print "- You cannot manage Dreamland configurations"
    fi
    ui_print "- Please install new Dreamland Manager"
  fi
fi

ui_print "- Extracting module files"
unzip -oj "$ZIPFILE" module.prop uninstall.sh 'common/*' -d "$MODPATH" >&2 || abort "! Can't extract module files: $?"
unzip -o "$ZIPFILE" 'system/*' -d "$MODPATH" >&2 || abort "! Can't extract the system folder: $?"

if [[ "$IS64BIT" == "false" ]]; then
  ui_print "- Removing 64-bit libraries"
  rm -rf "$MODPATH/system/lib64"
fi

ui_print "- Extracting riru files"
[[ -d "$RIRU_MODULE_PATH" ]] || mkdir -p "$RIRU_MODULE_PATH" || abort "! Can't create $RIRU_MODULE_PATH: $?"
cp -f "$TMPDIR/module.prop" "$RIRU_MODULE_PATH/module.prop" || abort "! Can't copy module.prop to $RIRU_MODULE_PATH: $?"

ui_print "- Preparing local directory"
[[ -d "$DREAMLAND_PATH" ]] || mkdir -p "$DREAMLAND_PATH" || abort "! Can't create $DREAMLAND_PATH: $?"

if [[ "$MAGISK_VER_CODE" -lt 20200 ]]; then
  ui_print "- Removing sepolicy because of Magisk version $MAGISK_VER is lower than 20.2"
  ui_print "- We recommend that you upgrade Magisk to the latest version"
  rm -f "$MODPATH/sepolicy.rule"
fi

# before Magisk 16e4c67, sepolicy.rule is copied on the second reboot
if [[ "$MAGISK_VER_CODE" -lt 21006 ]]; then
  ui_print "- Magisk version below 21006"
  ui_print "- You may need to manually reboot twice"
fi

ui_print "- Setting permissions"
# The following is the default rule, DO NOT remove
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm_recursive "$DREAMLAND_PATH" 1000 1000 0700 0600 u:object_r:system_data_file:s0
