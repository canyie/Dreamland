##########################################################################################
#
# Magisk Module Installer Script
#
##########################################################################################
##########################################################################################
#
# Instructions:
#
# 1. Place your files into system folder (delete the placeholder file)
# 2. Fill in your module's info into module.prop
# 3. Configure and implement callbacks in this file
# 4. If you need boot scripts, add them into common/post-fs-data.sh or common/service.sh
# 5. Add your additional or modified system properties into common/system.prop
#
##########################################################################################

##########################################################################################
# Config Flags
##########################################################################################

# Set to true if you do *NOT* want Magisk to mount
# any files for you. Most modules would NOT want
# to set this flag to true
SKIPMOUNT=false

# Set to true if you need to load system.prop
PROPFILE=true

# Set to true if you need post-fs-data script
POSTFSDATA=true

# Set to true if you need late_start service script
LATESTARTSERVICE=false

##########################################################################################
# Replace list
##########################################################################################

# List all directories you want to directly replace in the system
# Check the documentations for more info why you would need this

# Construct your own list here
REPLACE="
"

##########################################################################################
# Global varibles
##########################################################################################

RIRU_PATH=/data/misc/riru
RIRU_MODULE_PATH=$RIRU_PATH/modules/dreamland
DREAMLAND_PATH=/data/misc/dreamland

##########################################################################################
#
# Function Callbacks
#
# The following functions will be called by the installation framework.
# You do not have the ability to modify update-binary, the only way you can customize
# installation is through implementing these functions.
#
# When running your callbacks, the installation framework will make sure the Magisk
# internal busybox path is *PREPENDED* to PATH, so all common commands shall exist.
# Also, it will make sure /data, /system, and /vendor is properly mounted.
#
##########################################################################################
##########################################################################################
#
# The installation framework will export some variables and functions.
# You should use these variables and functions for installation.
#
# ! DO NOT use any Magisk internal paths as those are NOT public API.
# ! DO NOT use other functions in util_functions.sh as they are NOT public API.
# ! Non public APIs are not guranteed to maintain compatibility between releases.
#
# Available variables:
#
# MAGISK_VER (string): the version string of current installed Magisk
# MAGISK_VER_CODE (int): the version code of current installed Magisk
# BOOTMODE (bool): true if the module is currently installing in Magisk Manager
# MODPATH (path): the path where your module files should be installed
# TMPDIR (path): a place where you can temporarily store files
# ZIPFILE (path): your module's installation zip
# ARCH (string): the architecture of the device. Value is either arm, arm64, x86, or x64
# IS64BIT (bool): true if $ARCH is either arm64 or x64
# API (int): the API level (Android version) of the device
#
# Availible functions:
#
# ui_print <msg>
#     print <msg> to console
#     Avoid using 'echo' as it will not display in custom recovery's console
#
# abort <msg>
#     print error message <msg> to console and terminate installation
#     Avoid using 'exit' as it will skip the termination cleanup steps
#
# set_perm <target> <owner> <group> <permission> [context]
#     if [context] is empty, it will default to "u:object_r:system_file:s0"
#     this function is a shorthand for the following commands
#       chown owner.group target
#       chmod permission target
#       chcon context target
#
# set_perm_recursive <directory> <owner> <group> <dirpermission> <filepermission> [context]
#     if [context] is empty, it will default to "u:object_r:system_file:s0"
#     for all files in <directory>, it will call:
#       set_perm file owner group filepermission context
#     for all directories in <directory> (including itself), it will call:
#       set_perm dir owner group dirpermission context
#
##########################################################################################
##########################################################################################
# If you need boot scripts, DO NOT use general boot scripts (post-fs-data.d/service.d)
# ONLY use module scripts as it respects the module status (remove/disable) and is
# guaranteed to maintain the same behavior in future Magisk releases.
# Enable boot scripts by setting the flags in the config section above.
##########################################################################################

extract_sepolicy_rule_failed() {
  ui_print "- Extract sepolicy.rule failed, but don't be afraid"
  ui_print "- We will live patch the sepolicy at next boot in the post-fs-data script"
}

# Set what you want to display when installing your module

print_modname() {
  ui_print "- *******************************"
  ui_print "- *   Dreamland Magisk Module   *"
  ui_print "- *         by @canyie          *"
  ui_print "- *      Powered by Magisk      *"
  ui_print "- *******************************"
}

# Copy/extract your module files into $MODPATH in on_install.

on_install() {
  if [[ $ARCH != "arm64" && $ARCH != "arm" ]]; then
    abort "! Unsupported architecture: $ARCH"
  else
    ui_print "- Device architecture: $ARCH"
  fi

  if [[ $API -lt 24 ]]; then
    abort "! Unsupported Android API level $API"
  else
    ui_print "- Android API level: $API"
  fi

  [[ -f $RIRU_PATH/api_version ]] || [[ -f $RIRU_PATH/api_version.new ]] || abort "! Requirement module 'Riru - Core' is not installed" 

  RIRU_API_VERSION=$(cat $RIRU_PATH/api_version.new) || RIRU_API_VERSION=$(cat $RIRU_PATH/api_version) || RIRU_API_VERSION=0
  ui_print "- Riru API version: $RIRU_API_VERSION"

  RIRU_MIN_API=$(grep_prop api $TMPDIR/module.prop)
  [[ $RIRU_API_VERSION -ge $RIRU_MIN_API ]] || abort "! Unsupported Riru API version $RIRU_API_VERSION"

  if [[ $(pm path top.canyie.dreamland.manager) == "" ]]; then
    if [[ $(pm path top.canyie.dreamland.manager) != "" ]]; then
      ui_print "- Detected deprecated dreamland manager"
      ui_print "- It is not compatible with current framework version"
    else
      ui_print "- Dreamland Manager not found"
      ui_print "- You cannot manage Dreamland configurations"
    fi
    ui_print "- Please install new Dreamland Manager"
  fi
  
  ui_print "- Extracting module files"
  unzip -o $ZIPFILE system/* -d $MODPATH >&2 || abort "! Can't extract system/: $?"

  ui_print "- Extracting riru files"
  [[ -d $RIRU_MODULE_PATH ]] || mkdir -p $RIRU_MODULE_PATH || abort "! Can't create $RIRU_MODULE_PATH: $?"
  cp -f $TMPDIR/module.prop $RIRU_MODULE_PATH/module.prop || abort "! Can't copy module.prop to $RIRU_MODULE_PATH: $?"

  ui_print "- Preparing local directory"
  [[ -d $DREAMLAND_PATH ]] || mkdir -p $DREAMLAND_PATH || abort "! Can't create $DREAMLAND_PATH: $?"

  if [[ $MAGISK_VER_CODE -ge 20200 ]]; then
    ui_print "- Extracting sepolicy.rule for Magisk $MAGISK_VER"
    cp -f $TMPDIR/sepolicy.rule $MODPATH/sepolicy.rule || extract_sepolicy_rule_failed
  else
    ui_print "- Magisk version $MAGISK_VER is lower than 20.2"
    ui_print "- We recommend that you upgrade Magisk to the latest version"
  fi
}

# Only some special files require specific permissions
# This function will be called after on_install is done
# The default permissions should be good enough for most cases

set_permissions() {
  # The following is the default rule, DO NOT remove
  set_perm_recursive $MODPATH 0 0 0755 0644

  set_perm_recursive $DREAMLAND_PATH 1000 1000 0700 0600 u:object_r:system_data_file:s0
}
