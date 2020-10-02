#!/system/bin/sh
MODDIR=${0%/*}
DATADIR=/data/misc/dreamland
[[ -f $DATADIR/disable_reason ]] && mv -f $DATADIR/disable_reason $DATADIR/disable

[[ -f $DATADIR/disable ]] && exit 0

[[ -f $DATADIR/bootloop_protection ]] || exit 0

MAIN_ZYGOTE_NICENAME=zygote
CPU_ABI=$(getprop ro.product.cpu.api)
[[ $CPU_ABI == "arm64-v8a" ]] && MAIN_ZYGOTE_NICENAME=zygote64

sleep 5

ZYGOTE_PID1=$(pidof $MAIN_ZYGOTE_NICENAME)
sleep 20
ZYGOTE_PID2=$(pidof $MAIN_ZYGOTE_NICENAME)
sleep 20
ZYGOTE_PID3=$(pidof $MAIN_ZYGOTE_NICENAME)

[[ $ZYGOTE_PID1 == $ZYGOTE_PID2 ]] && [[ $ZYGOTE_PID2 == $ZYGOTE_PID3 ]] && exit 0

sleep 20
ZYGOTE_PID4=$(pidof $MAIN_ZYGOTE_NICENAME)
[[ $ZYGOTE_PID3 == $ZYGOTE_PID4 ]] && exit 0

# Zygote keeps restarting in 65s, disable framework and restart zygote

echo "Bootloop protection: zygote keeps restarting in 65s" >> $DATADIR/disable
setprop ctl.restart zygote
