#!/usr/bin/env bash

unzipper="unzipper/assembly"
parser="parser/assembly"
spam_filter="spam-filter/assembly spam-filter/assemblyPackageDependency"

projects=(${unzipper} ${parser} ${spam_filter})

for project in "${projects[@]}" ; do
    sbt_line=${sbt_line}" "${project}
done

sbt ${sbt_line}
