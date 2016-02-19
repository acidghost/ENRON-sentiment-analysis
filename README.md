# ENRON sentiment analysis
###### Large Scale Data Engineering assignment - VU MSc AI

Steps needed:
- find a suitable form for the data (possibly SparkSQL tables?);
- preprocess data and put them in that form;
- tag each email with sentiment using Stanford CoreNLP;
- get and plot some summary stats about the data;
- find enron stock prices<sup>1</sup>;
- summarize sentiment per day;
- plot sentiment per day compared to stock prices.

---

> <sup>1</sup> Found only in PDF for now... [here](http://law2.umkc.edu/faculty/projects/ftrials/enron/enronstockchart.pdf) and [here](http://www.gilardi.com/pdf/enro13ptable.pdf)


### Setup
1. `git clone https://github.com/acidghost/ENRON-sentiment-analysis.git`  
2. `git submodule update --depth 1 --init --recursive`  
3. `./hathi-client/bin/get.sh hadoop`  
4. `./hathi-client/bin/get.sh spark`

### Usage
1. `eval $(./hathi-client/bin/env.sh)`
2. `kinit USRNAME`

#### Bugs / problems
To use `spark-shell` you need to change the file `./hathi-client/conf/spark/spark-env.sh` and add the spark option `-Dscala.usejavacp=true`.
A pull request has already been made to fix this in the hathi-client repo.


