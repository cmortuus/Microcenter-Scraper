#!/bin/sh

if [ "root" != "$USER" ]; then
  su -c "$0" root
  exit
fi

yes | apt update
yes | apt upgrade
yes | apt install default-jdk
snap install kotlin --classic
# TODO add the stuff for mysql