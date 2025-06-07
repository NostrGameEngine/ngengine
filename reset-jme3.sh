#!/bin/bash





git remote add jmeupstream https://github.com/jMonkeyEngine/jmonkeyengine.git || true

git fetch jmeupstream


FOLDERS="jme3-android jme3-android-examples jme3-awt-dialogs \
jme3-core jme3-desktop jme3-effects jme3-examples jme3-ios \
jme3-jbullet jme3-jogg jme3-lwjgl3 jme3-networking jme3-plugins \
jme3-plugins-json jme3-plugins-json-gson jme3-screenshot-tests jme3-terrain\
jme3-testdata"
for folder in $FOLDERS; do
  git checkout jmeupstream/master -- "$folder"
done

echo "✅ Folders updated to match 'jmeupstream/master'. No commit has been made."
