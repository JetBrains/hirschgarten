if ! command -v vncserver &> /dev/null
then
    apt-get update
    apt-get install -y expect xfce4 tigervnc-standalone-server
fi

mkdir -p ~/.vnc

cat > ~/.vnc/config << EOF
session=xfce4-session
geometry=1920x1080
localhost
alwaysshared
EOF

if [ ! -d /mnt/jetbrains/system/noVNC ]; then
    git clone https://github.com/novnc/noVNC /mnt/jetbrains/system/noVNC
fi
