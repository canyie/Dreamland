
# only for Android 7.0+
if [ "$API" -lt 24 ]; then
  abort "! Unsupported sdk: $API"
else
  ui_print "- Device sdk: $API"
fi

LIBRARIES_TXT="/system/etc/public.libraries.txt"

# ui_print "- $MODPATH"

mkdir /p "$MODPATH/system/etc"
cp -f "$LIBRARIES_TXT" "$MODPATH/system/etc/" || abort "! Failed to copy $LIBRARIES_TXT"
LIBRARIES_TXT="$MODPATH/system/etc/public.libraries.txt"


cat "$LIBRARIES_TXT" | grep -v '#' | grep 'libdreamland.so' > /dev/null 2>&1 || \
echo 'libdreamland.so' >> "$LIBRARIES_TXT"

ui_print "- Extracting module file ..."
unzip -o "$ZIPFILE" 'system/*' -d "$MODPATH" || abort "! Failed to unzip module file"

[ ! "$IS64BIT" ] && rm -rf "$MODPATH/system/lib64"


ui_print "- Setting permissions"
set_perm_recursive "$MODPATH" 0 0 0755 0644

