#! /bin/bash

#
#  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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

#
# IMPORTANT: there is no error handling on this script so ensure the docker builds/pushes worked.
#

#
# This script assumes you are in the root directory of the project,
# you have git and maven, you set the environment varaible `release` and
# that you already ran the following command:
# $ mvn release:prepare release:perform
#
# To launch a full release in one line, use (don't forget to set the release you are doingg):
# $ mvn release:prepare release:perform && release=1.0.3 ./post-release.sh
#

echo "Pushing tags"
git reset --hard
git push --follow-tags

if [ "x${release}" == "x" ]; then
    echo "No \$release, set it before running this script please"
    exit 1
fi

echo "Building tag $release"
git checkout -b component-runtime-$release component-runtime-$release
mvn clean install -DskipTests -Dinvoker.skip=true -T1C

echo "Building and pushing Starter Component Server image"
cd .docker
    docker build --build-arg SERVER_VERSION=$release --build-arg ARTIFACT_ID=component-starter-server --tag tacokit/component-starter-server:$release . && \
    docker push tacokit/component-starter-server:$release
cd -

echo "Building and pushing Component Server image"
COMPONENT_SERVER_DOCKER_BUILD_ONLY=true ./.travis/docker.sh && \
tacokit/component-server:$release

echo "Rebuilding master"
git reset --hard
git checkout master
mvn clean install -DskipTests -Dinvoker.skip=true -T1C && \
git commit -a -m "Updating doc for next iteration" && \
git push

echo "Updating the documentation for next iteration"
cd documentation
    mvn clean install pre-site -Pgh-pages
cd -
