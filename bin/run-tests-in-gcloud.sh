#!/bin/bash

if [ ! $# -eq 5 ]; then
    SELF=`basename $0`
    echo "Usage: $SELF <APK_RUNNER_APP> <APK_RUNNER_ANDROID_TEST> <RESULTS_DIR> <ARTIFACTS_DIR> <RENAME_SUFFIX>"
    exit 1
fi

APK_RUNNER_APP=$1
APK_RUNNER_ANDROID_TEST=$2
RESULTS_DIR=$3
ARTIFACTS_DIR=$4
RENAME_SUFFIX=$5

LOG_FILE="$RESULTS_DIR/gcloud_output.txt"

rm -rf "$RESULTS_DIR"
mkdir -p "$RESULTS_DIR"

echo "gcloud --version"
gcloud --version

gcloud firebase test android run \
        --type instrumentation \
        --timeout=30m \
        --app "$APK_RUNNER_APP" \
        --test "$APK_RUNNER_ANDROID_TEST" \
        --device model=Pixel2,version=26,orientation=portrait \
        --device model=Pixel2,version=26,orientation=landscape \
        --device model=Pixel2,version=27,orientation=portrait \
        --device model=Pixel2,version=27,orientation=landscape \
        --device model=Pixel2,version=28,orientation=portrait \
        --device model=Pixel2,version=28,orientation=landscape \
        --device model=Pixel2,version=29,orientation=portrait \
        --device model=Pixel2,version=29,orientation=landscape \
        --device model=flo,version=21,orientation=portrait \
        --device model=flo,version=21,orientation=landscape \
        --device model=hammerhead,version=23,orientation=portrait \
        --device model=hammerhead,version=23,orientation=landscape \
        --device model=lucye,version=24,orientation=portrait \
        --device model=lucye,version=24,orientation=landscape \
        --device model=G8142,version=25,orientation=portrait \
        --device model=G8142,version=25,orientation=landscape \
        --device model=walleye,version=27,orientation=portrait \
        --device model=walleye,version=27,orientation=landscape \
        --device model=walleye,version=28,orientation=portrait \
        --device model=walleye,version=28,orientation=landscape 2>&1 | tee $LOG_FILE

EXIT_CODE=${PIPESTATUS[0]}
echo "gcloud finished with code $EXIT_CODE"

GS_FOLDER=`cat $LOG_FILE | grep "Raw results will be stored in your GCS bucket at" | sed "s/.*\/\(test-lab-.*\)\/.*/\1/"`
echo GS_FOLDER=$GS_FOLDER

gsutil -m cp -r "gs://$GS_FOLDER/*" "$RESULTS_DIR" || EXIT_CODE=$?
echo "gsutil finished with code $?"

# Add a suffix to folder names generated by the firebase test cloud in order to 
# be more informative when displayed in circleci's test summary
find "$RESULTS_DIR" -depth -mindepth 1 -maxdepth 1 -type d -exec mv {} {}$RENAME_SUFFIX \;

cd "$RESULTS_DIR"

rm `basename $APK_RUNNER_APP`
rm `basename $APK_RUNNER_ANDROID_TEST`

mkdir -p "$ARTIFACTS_DIR"
tar czf "$ARTIFACTS_DIR/gcloud_results.tar.gz" -C "$RESULTS_DIR" .

# keep only test_result.xml files, otherwise circleci won't display a test summary
find "$RESULTS_DIR" -type f ! -name "*.xml" -delete
find "$RESULTS_DIR" -type d -empty -delete

exit $EXIT_CODE
