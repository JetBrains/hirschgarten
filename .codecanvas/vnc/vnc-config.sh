mkdir -p ~/.vnc

cat >~/.vnc/config <<EOF
session=xfce4-session
geometry=3840x2160
localhost
alwaysshared
EOF
