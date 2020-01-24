adb root
adb remount
adb push build\outputs\apk\debug\app-debug.apk /data/dreamland/core.apk
adb push build\outputs\apk\debug\libdreamland.so /system/lib
pause