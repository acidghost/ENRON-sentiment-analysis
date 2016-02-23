#!/usr/bin/env bash

source_url=http://www.aueb.gr/users/ion/data/enron-spam/preprocessed/

if [ -d "enron-spam" ] ; then
    echo "enron-spam folder already exists"
    exit 1
fi
mkdir -p enron-spam

cd enron-spam
for i in 1 2 3 4 5 6 ; do
    wget "http://www.aueb.gr/users/ion/data/enron-spam/preprocessed/enron"${i}".tar.gz"
    tar -xzvf "enron"${i}".tar.gz"
    rm "enron"${i}".tar.gz"
done
cd ..
