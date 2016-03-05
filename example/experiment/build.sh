#! /usr/bin/env bash

#-------------------------------------------------------------------------#
#                                build.sh                                 #
#                                                                         #
# Author: Nic H.                                                          #
# Date: 2016-Mar-05                                                       #
#                                                                         #
# prepare the necessary files used to produce the cauliflower             #
# experimental set                                                        #
#-------------------------------------------------------------------------#

set -u
set -e

#
# Define some useful vars
#
. $(dirname $0)/common.sh

#
# Make sure the environment is set up correctly
#
[ -f $CCLYSER_HOME/bin/fact-generator ] || (echo "Cannot find the fact-generator, cclyzer needs to be built" >&2 && exit 1)
[ -f $EXPER_DIR/gigascale* ] || (echo "Cannot find the gigascale datasets, please download them to this directory" >&2 && exit 1)
export PATH="$PATH:$EXPER_DIR" # this contains spec_intercept.sh, used to compile spec benchmarks into .bc files
which_or_bail gradle
which_or_bail java
which_or_bail make
which_or_bail gcc
which_or_bail clang
which_or_bail clang++
which_or_bail llc
which_or_bail llvm-dis
which_or_bail runspec
which_or_bail spec_intercept.sh

#
# Clean up prior build
#
rm -rf $SPOUT_DIR $CCLYS_DIR $GIGAS_DIR $BUILT_ARCHIVE $CAULI_JAR
[ $# == 0 ] || exit 0 # user can clean the preparation by passing any argument to this script

#
# Build cauliflower itself and the experiment source files
#
gradle -p $CAULI_DIR build
mv `find $CAULI_DIR/build -iname "cauliflower*.jar"` $CAULI_JAR
mkdir -p $GIGAS_DIR
mkdir -p $CCLYS_DIR
java -jar $CAULI_JAR -p -sn $GIGAS_DIR/${GIGAS_NAME}.h -cs $GIGAS_DIR/${GIGAS_NAME}.cpp ${GIGAS_NAME}.cflr
java -jar $CAULI_JAR -p -t -sn $GIGAS_DIR/${GIGAS_NAME}_t.h -cs $GIGAS_DIR/${GIGAS_NAME}_t.cpp ${GIGAS_NAME}.cflr
java -jar $CAULI_JAR -a Souffle -sn $CCLYS_DIR/${CCLYS_NAME}.h -cs $CCLYS_DIR/${CCLYS_NAME}.cpp ${CCLYS_NAME}.cflr
cp $GIGAS_DIR/* $CCLYS_DIR/* $CAULI_DIR/spikes/
make -C $CAULI_DIR -j4
mv $CAULI_DIR/bin/$CCLYS_NAME $CCLYS_DIR
mv $CAULI_DIR/bin/$GIGAS_NAME $GIGAS_DIR
mv $CAULI_DIR/bin/${GIGAS_NAME}_t $GIGAS_DIR

#
# Prepare gigascale benchmarks
#
mkdir -p ${GIGAS_DIR}_TMP
echo "extracting gigascale datasets"
tar xf $EXPER_DIR/gigascale* -C ${GIGAS_DIR}_TMP
GIGAS_DATASETS=`find ${GIGAS_DIR}_TMP -type d -name "datasets"`
[ -n "$GIGAS_DATASETS" ] || (echo "unable to locate the datasets in your version of gigascale" >&2 && exit 1)
mv ${GIGAS_DIR}_TMP/* $GIGAS_DIR/gigascale
find $GIGAS_DIR/gigascale -name "*.java" | xargs sed -i -e 's/\xe2\x80\x9c/"/g' -e 's/\xe2\x80\x9d/"/g' -e 's/\xe2\x80\x99/"/g' # remove unhelpful unicode encoding of the license
for VPT in `find $GIGAS_DIR -name "VarPointsTo.csv"`; do mv $VPT ${VPT%csv}ans; done # prevent cauliflower from reading the output relation as a csv file
rmdir ${GIGAS_DIR}_TMP

#
# Prepare spec benchmarks
#
rm -f $SPEC/config/cauliflower.cfg
cp $EXPER_DIR/spec_config.cfg $SPEC/config/cauliflower.cfg
runspec --action=build --config=cauliflower $SPEC_BENCHES
for BC in `find $SPOUT_DIR -name "*.bc"`; do mv $BC $CCLYS_DIR/; done
(cd $SPOUT_DIR; zip -rq $CCLYS_DIR/spec_build.zip *)
$EXPER_DIR/cclyser_convert.sh $CCLYS_DIR/*.bc

#
# Create an archive of the built files for use on another platform
#
GIGAS_REL=`python -c "import os.path; print os.path.relpath('$GIGAS_DIR/', '.')"`
CCLYS_REL=`python -c "import os.path; print os.path.relpath('$CCLYS_DIR/', '.')"`
CAULI_REL=`python -c "import os.path; print os.path.relpath('$CAULI_JAR', '.')"`
zip -r $BUILT_ARCHIVE $GIGAS_REL/ $CCLYS_REL/ $CAULI_REL
