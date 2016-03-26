package nl.vu.ai.lsde.enron.etl

import java.util.Properties

import edu.stanford.nlp.pipeline.StanfordCoreNLP


object NLP {

    // load sentiment annotator pipeline
    val nlpProps = new Properties
    // nlpProps.setProperty("annotators", "tokenize, ssplit, pos, parse, lemma, sentiment")
    nlpProps.setProperty("annotators", "tokenize ssplit pos parse sentiment")
    nlpProps.setProperty("tokenize.options", "untokenizable=allDelete")
    nlpProps.setProperty("tokenize.options", "ptb3Escaping=true")
    nlpProps.setProperty("pos.maxlen", "100")
    nlpProps.setProperty("pos.model", "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger")
    nlpProps.setProperty("parse.maxlen", "100")
    nlpProps.setProperty("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz")
    @transient lazy val pipeline = new StanfordCoreNLP(nlpProps)

}
