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

set_directory_permission_or_die() {
    chmod -R $2 $1 || abort "chmod($1) failed"
}

set_owner_or_die() {
    chown $2:$3 $1 || abort "chown($1) failed"
}

set_directory_owner_or_die() {
    chown -R $2:$3 $1 || abort "chown($1) failed"
}

set_context_or_die() {
    if [ $is_selinux_enabled -eq 1 ]; then
        chcon $2 $1 || abort "chcon($1) failed"
    fi
}

set_directory_context_or_die() {
    if [ $is_selinux_enabled -eq 1 ]; then
        chcon -R $2 $1 || abort "chcon($1) failed"
    fi
}

set_system_file() {
    set_owner_or_die "/system/$1" root root
    set_permission_or_die "/system/$1" 644
    set_context_or_die "/system/$1" "u:object_r:system_file:s0"
}

copy_file_to_system() {
    cp "system/$1" "/system/$1" || abort "cp(/system/$1) failed"
    set_system_file $1
}

echo "#######################"
echo "#                     #"
echo "# Dreamland Installer #"
echo "#                     #"
echo "#######################"

uid=$(id -u)
if [ $uid -ne 0 ]; then
    abort "Dreamland installer should run as root!"
fi

if [ ! -d "/data/dreamland/" ]; then
    echo "Creating directory /data/dreamland/ ..."
    mkdir -p "/data/dreamland/" || abort "mkdir(/data/dreamland/) failed"
fi

set_directory_owner_or_die /data/dreamland/ $manager_uid $manager_gid
set_directory_permission_or_die /data/dreamland/ 755
set_directory_context_or_die /data/dreamland/ $manager_file_secontext

if [ ! -d "/data/dreamland/backup/" ]; then
    mkdir -p "/data/dreamland/backup/" || abort "mkdir(/data/dreamland/backup/) failed"
fi
set_directory_owner_or_die /data/dreamland/backup/ $manager_uid $manager_gid
set_directory_permission_or_die /data/dreamland/backup/ 700

echo "Mounting /system read-write..."

mount -o rw,remount /system || abort "failed to mount /system read-write"

echo "Copying dreamland files..."

copy_file_to_system lib/libdreamland.so
if [ -d "/system/lib64" ]; then
    copy_file_to_system lib64/libdreamland.so
fi

copy_file_to_system framework/dreamland.jar

if [ ! -f "/data/dreamland/backup/public.libraries.txt" ]; then
    echo "Backing up /system/etc/public.libraries.txt ..."
    cp "/system/etc/public.libraries.txt" "/data/dreamland/backup/public.libraries.txt" || abort "backup public.libraries.txt failed"
    
    echo "Appending core so path to public.libraries.txt ..."
    if [ $(tail -n1 /system/etc/public.libraries.txt | wc -l) -eq 1 ]; then
        # has a new line end of the file
        append="libdreamland.so"
    else
        append="\nlibdreamland.so"
    fi
    echo $append >> /system/etc/public.libraries.txt || abort "write public.libraries.txt failed"
    # Ensure that /system/etc/public.libraries.txt has the correct owner, permissions, and SELinux context.
    # TODO: try restore /system/etc/public.libraries.txt when set owner/permission/context failed.
    set_system_file etc/public.libraries.txt
fi

echo "All done."

exit 0