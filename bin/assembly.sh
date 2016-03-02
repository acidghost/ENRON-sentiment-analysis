#!/usr/bin/env bash

unzipper="unzipper/assembly"
etl="etl/assembly etl/assemblyPackageDependency"
spam_filter="spam-filter/assembly spam-filter/assemblyPackageDependency"
sentiment_resumer="sentiment-resumer/assembly"

projects=(${unzipper} ${etl} ${spam_filter} ${sentiment_resumer})

for project in "${projects[@]}" ; do
    sbt_line=${sbt_line}" "${project}
done

set -x
sbt ${sbt_line}
set +x
