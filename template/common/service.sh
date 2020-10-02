#!/system/bin/sh
MODDIR=${0%/*}

[[ -f /data/misc/dreamland/disable ]] && exit 0

[[ -f /data/misc/dreamland/bootloop_protection ]] || exit 0

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

touch /data/misc/dreamland/disable
echo "Bootloop protection: zygote keeps restarting in 65s" >> /data/misc/dreamland/disable_reason
setprop ctl.restart zygote
