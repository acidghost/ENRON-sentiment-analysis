# ENRON sentiment analysis
###### Large Scale Data Engineering assignment - VU MSc AI

[Project's notes in Google Drive](https://docs.google.com/document/d/1EWcemUePsjHuGxTtwwh674qPbz1Hb0OI6r4YqnVsMz0/edit)


### Setup
1. `git clone https://github.com/acidghost/ENRON-sentiment-analysis.git`  
2. `git submodule update --depth 1 --init --recursive`  
3. `./hathi-client/bin/get.sh hadoop`  
4. `./hathi-client/bin/get.sh spark`
5. Install the [Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files](http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html)

### Usage
1. `eval $(./hathi-client/bin/env.sh)`
2. `kinit USRNAME`


