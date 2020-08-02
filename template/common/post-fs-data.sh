#!/system/bin/sh

MODDIR=${0%/*}

[[ -f ${MODDIR}/sepolicy.rule ]] && exit 0

magiskpolicy --live "allow zygote zygote process { execmem }" \
    "allow system_server system_server process { execmem }"
