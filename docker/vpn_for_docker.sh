#!/bin/bash

set -x

if [ "$(id -u)" -ne 0 ]; then
    echo "This script must be run as root. Use sudo."
    exit 1
fi

DIR="/tmp/bydmate"
VPN_DEV="$1"
VPN_IP="$2"

mkdir -p $DIR && cd $DIR

# 创建路由表
echo "200 dockertun" | tee -a /etc/iproute2/rt_tables

# 配置路由
ip route add default via $VPN_IP dev $VPN_DEV table dockertun

# 设置策略路由
sudo ip rule add fwmark 0x1 lookup dockertun

# 创建cgroup目录
mkdir net_cls
mount -t cgroup -o net_cls net_cls net_cls
mkdir -p net_cls/docker_tun
echo 0x1212123 > net_cls/docker_tun/net_cls.classid

# 移动Docker进程到cgroup
find /sys/fs/cgroup/system.slice/docker.service -name cgroup.procs \
    | xargs -I{} cat {} \
    | xargs -I{} bash -c "echo {} > net_cls/docker_tun/cgroup.procs"

# 配置iptables规则
iptables -t mangle -A OUTPUT -m cgroup --cgroup 0x1212123 -j MARK --set-mark 1
iptables -t mangle -A PREROUTING -m conntrack --ctstate RELATED,ESTABLISHED -j CONNMARK --restore-mark
iptables -t mangle -A OUTPUT -m conntrack --ctstate RELATED,ESTABLISHED -j CONNMARK --restore-mark


