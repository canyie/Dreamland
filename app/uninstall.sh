echo_error() {
    echo $1 >&2
}

abort() {
    echo_error "! $1"
    echo_error "! aborting..."
    exit 1
}

set_permission_or_die() {
    chmod $2 $1 || abort "chmod($1) failed"
}

set_owner_or_die() {
    chown $2:$3 $1 || abort "chown($1) failed"
}

set_context_or_die() {
    if [ $is_selinux_enabled -eq 1 ]; then
        chcon $2 $1 || abort "chcon($1) failed"
    fi
}

uid=$(id -u)
if [ $uid -ne 0 ]; then
    abort "Dreamland uninstaller should run as root!"
fi

echo "Mounting /system read-write..."

mount -o rw,remount /system || abort "failed to mount /system read-write"

if [ -f "/data/dreamland/backup/public.libraries.txt" ]; then
    echo "Restoring /system/etc/public.libraries.txt ..."
    mv -f /data/dreamland/backup/public.libraries.txt /system/etc/public.libraries.txt || abort "failed to restore public.libraries.txt"
    set_owner_or_die "/system/etc/public.libraries.txt" root root
    set_permission_or_die "/system/etc/public.libraries.txt" 644
    set_context_or_die "/system/etc/public.libraries.txt" "u:object_r:system_file:s0"
fi

if [ -f "/system/lib/libdreamland.so" ]; then
    echo "Deleting dreamland's core so(32 bits)..."
    rm /system/lib/libdreamland.so || abort "delete /system/lib/libdreamland.so failed"
fi

if [ -f "/system/lib64/libdreamland.so" ]; then
    echo "Deleting dreamland's core so(64 bits)..."
    rm /system/lib64/libdreamland.so || abort "delete /system/lib64/libdreamland.so failed"
fi

if [ -f "/system/framework/dreamland.jar" ]; then
    echo "Deleting dreamland's core jar..."
    rm /system/framework/dreamland.jar || abort "delete /system/framework/dreamland.jar failed"
fi

if [ -d "/data/dreamland/" ]; then
    echo "Deleting dreamland's data directory..."
    rm -r /data/dreamland/ || abort "delete /data/dreamland/ failed"
fi

echo "All done."

exit 0
