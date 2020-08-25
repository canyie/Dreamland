#!/system/bin/sh
MODDIR=${0%/*}

[[ -f /data/misc/dreamland/disable ]] && exit 0

ZYGOTE_PID1=$(pidof zygote)
sleep 20
ZYGOTE_PID2=$(pidof zygote)
sleep 20
ZYGOTE_PID3=$(pidof zygote)

[[ $ZYGOTE_PID1 == $ZYGOTE_PID2 ]] && [[ $ZYGOTE_PID2 == $ZYGOTE_PID3 ]] && exit 0

sleep 20
ZYGOTE_PID4=$(pidof zygote)
[[ $ZYGOTE_PID3 == $ZYGOTE_PID4 ]] && exit 0

# Zygote keeps restarting in 60s, disable framework and restart zygote

touch /data/misc/dreamland/disable
echo "Bootloop protection: zygote keeps restarting in 60s" >> /data/misc/dreamland/disable_reason
setprop ctl.restart zygote
