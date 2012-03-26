#!/bin/bash -x

if [[ -z "$1"  ]]; then
echo "Usage: install-binaries.sh image-name offset"
echo "To find offset, run 'parted file.img'"
echo "1) Enter 'unit', then 'b'"
echo "2) Enter 'print'"
echo "3) Use the offset of the second partition, with the B removed from the end"
echo "4) 'quit' to quit GNU parted"
exit -1
fi

FILE=`pwd`/$1

mkdir -p /tmp/binaries
cd /tmp/binaries/
wget --no-check-certificate https://dl.google.com/dl/android/aosp/imgtec-panda-iml74k-cfb7bdad.tgz
tar -zxvf imgtec-panda-iml74k-cfb7bdad.tgz
sh extract-imgtec-panda.sh
sudo mount -o loop,offset=$2 $FILE /mnt/
sudo mkdir -p /mnt/vendor/bin/
sudo mkdir -p /mnt/vendor/lib/egl
sudo cp vendor/imgtec/panda/proprietary/pvrsrvinit /mnt/vendor/bin/pvrsrvinit
sudo cp vendor/imgtec/panda/proprietary/pvrsrvinit /mnt/bin/pvrsrvinit
sudo chmod a+x /mnt/vendor/bin/pvrsrvinit
sudo cp vendor/imgtec/panda/proprietary/libEGL_POWERVR_SGX540_120.so /mnt/vendor/lib/egl/libEGL_POWERVR_SGX540_120.so
sudo cp vendor/imgtec/panda/proprietary/libGLESv1_CM_POWERVR_SGX540_120.so /mnt/vendor/lib/egl/libGLESv1_CM_POWERVR_SGX540_120.so
sudo cp vendor/imgtec/panda/proprietary/libGLESv2_POWERVR_SGX540_120.so /mnt/vendor/lib/egl/libGLESv2_POWERVR_SGX540_120.so
sudo cp vendor/imgtec/panda/proprietary/gralloc.omap4.so /mnt/vendor/lib/hw/gralloc.omap4.so
sudo cp vendor/imgtec/panda/proprietary/libglslcompiler.so /mnt/vendor/lib/libglslcompiler.so
sudo cp vendor/imgtec/panda/proprietary/libIMGegl.so /mnt/vendor/lib/libIMGegl.so
sudo cp vendor/imgtec/panda/proprietary/libpvr2d.so /mnt/vendor/lib/libpvr2d.so
sudo cp vendor/imgtec/panda/proprietary/libpvrANDROID_WSEGL.so /mnt/vendor/lib/libpvrANDROID_WSEGL.so
sudo cp vendor/imgtec/panda/proprietary/libPVRScopeServices.so /mnt/vendor/lib/libPVRScopeServices.so
sudo cp vendor/imgtec/panda/proprietary/libsrv_init.so /mnt/vendor/lib/libsrv_init.so
sudo cp vendor/imgtec/panda/proprietary/libsrv_um.so /mnt/vendor/lib/libsrv_um.so
sudo cp vendor/imgtec/panda/proprietary/libusc.so /mnt/vendor/lib/libusc.so
sudo chmod -R 755 /mnt/vendor/lib/
sudo umount -f $FILE
cd -
sudo rm -r /tmp/binaries
