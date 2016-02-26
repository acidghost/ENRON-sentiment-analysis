#!/usr/bin/env bash

unzipper="unzipper/assembly"
etl="etl/assembly etl/assemblyPackageDependency"
spam_filter="spam-filter/assembly spam-filter/assemblyPackageDependency"

projects=(${unzipper} ${etl} ${spam_filter})

for project in "${projects[@]}" ; do
    sbt_line=${sbt_line}" "${project}
done

sbt ${sbt_line}
