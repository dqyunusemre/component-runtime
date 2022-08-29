#!/usr/bin/env bash
#
#  Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

# set -xe

# Jacoco report generation
# $1: install_dir
# $2: coverage_dir

# check parameters
[ -z ${1+x} ] && usage "Parameter 'install_dir'"
[ -z ${1+x} ] && usage "Parameter 'coverage_dir'"
INSTALL_DIR=${1}
COVERAGE_DIR=${2}

DISTRIBUTION_DIR="${INSTALL_DIR}/component-server-distribution"
JACOCO_EXEC_PATH="${DISTRIBUTION_DIR}/jacoco.exec"
LIB_DIR="${DISTRIBUTION_DIR}/lib"
LIB_BACKUP_DIR="${COVERAGE_DIR}/lib_backup"
SOURCES_DIR=${COVERAGE_DIR}/src
JACOCO_CLI_PATH="${LIB_DIR}/jacococli.jar"

main() (
  echo "##############################################"
  echo "Jacoco report creation with:"
  echo "${JACOCO_CLI_PATH}"
  echo  "JACOCO_EXEC_PATH: ${JACOCO_EXEC_PATH}"
  echo  "LIB_BACKUP_DIR: ${LIB_BACKUP_DIR}"
  echo  "csv: ${COVERAGE_DIR}/report.csv"
  echo  "xml: ${COVERAGE_DIR}/report.xml"
  echo  "html: ${COVERAGE_DIR}/html"
  echo  "src: ${SOURCES_DIR}"
  echo "##############################################"

  jacoco_dump
  jacoco_report
)

function usage(){
  echo "Generate Jacoco report"
  echo "Usage : $0 <install_dir> <coverage_dir>"
  echo
  echo "$1 is needed."
  echo
  exit 1
}

function jacoco_dump {
  printf "\n# Jacoco dump\n"
  java -jar "${LIB_DIR}/jacococli.jar" \
    dump --destfile "${JACOCO_EXEC_PATH}"
    # not used yet --quiet
	echo "##############################################"
}

function jacoco_report {
  printf "\n# Jacoco report\n"
  java -jar "${LIB_DIR}/jacococli.jar" \
    report "${JACOCO_EXEC_PATH}" \
    --classfiles "${LIB_BACKUP_DIR}" \
    --csv "${COVERAGE_DIR}/report.csv" \
    --xml "${COVERAGE_DIR}/report.xml" \
    --html "${COVERAGE_DIR}/html" \
    --name "TCK API test coverage" \
    --sourcefiles "${SOURCES_DIR}"
    # not used yet --quiet
	echo "##############################################"
}

main "$@"
