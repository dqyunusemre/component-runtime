#! /bin/bash
#
#  Copyright (C) 2006-2021 Talend Inc. - www.talend.com
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

set -xe

main() {
  # force creation of gnupg folder by listing private keys and export it
  gpg -k
  export GPG_DIR="${HOME}/.gnupg"
  # work folders
  mkdir -p .build .build_source
  # decrypt keys
  openssl enc -d -aes256                       \
          -iv  "${encrypted_f13b410addf6_iv}"  \
          -K   "${encrypted_f13b410addf6_key}" \
          -in  .travis/encrypted.tar.gz.enc    \
          -out .build_source/encrypted.tar.gz
  tar -xzf .build_source/encrypted.tar.gz -C .build
  cp  -v   .build/gpg/travis.* "$GPG_DIR/"
  # import key
  gpg --import --no-tty --batch --yes "$GPG_DIR/travis.priv.bin"
}

main "$@"