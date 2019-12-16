#!/bin/bash
set -ev

openssl aes-256-cbc -K $encrypted_18bade7c6277_key -iv $encrypted_18bade7c6277_iv -in travis_id_rsa.enc -out travis_id_rsa -d
chmod 400 travis_id_rsa

rsync -Ihvz -e "ssh -i ./travis_id_rsa" $TRAVIS_BUILD_DIR/build/libs/SureBot.jar auto@server.suredroid.com:/opt/discord/surebot/

echo "multiuser on" >> screen.conf
echo "acladd root" >> screen.conf

rsync -hvz -e "ssh -i ./travis_id_rsa" $TRAVIS_BUILD_DIR/screen.conf auto@server.suredroid.com:/opt/discord/surebot/

ssh auto@server.suredroid.com -i ./travis_id_rsa /bin/bash << 'EOT'
  if screen -list | grep -q "discord"; then
    screen -X -S "discord" quit
  fi
  screen -dmS discord -c /opt/discord/surebot/screen.conf
  screen -S discord -p 0 -X stuff "java -jar /opt/discord/surebot/SureBot.jar^M"
EOT
