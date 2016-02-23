package nl.vu.ai.lsde.enron.spam

import java.util.Properties

import edu.stanford.nlp.process.Morphology
import edu.stanford.nlp.simple.Document

import scala.collection.JavaConversions._


object Tokenizer {

    // scalastyle:off line.size.limit
    val stopwords = "^[\\-*|\\,*|\\.*|\\_*|\\/*|\\@*|\\%*|\\$*|\\!*|\\?*|\\=*|\\:*|a|about|above|after|again|against|all|am|an|and|any|are|aren't|as|at|be|because|been|before|being|below|between|both|but|by|can't|cannot|could|couldn't|did|didn't|do|does|doesn't|doing|don't|down|during|each|few|for|from|further|had|hadn't|has|hasn't|have|haven't|having|he|he'd|he'll|he's|her|here|here's|hers|herself|him|himself|his|how|how's|i|i'd|i'll|i'm|i've|if|in|into|is|isn't|it|it's|its|itself|let's|me|more|most|mustn't|my|myself|no|nor|not|of|off|on|once|only|or|other|ought|our|ours|ourselves|out|over|own|same|shan't|she|she'd|she'll|she's|should|shouldn't|so|some|such|than|that|that's|the|their|theirs|them|themselves|then|there|there's|these|they|they'd|they'll|they're|they've|this|those|through|to|too|under|until|up|very|was|wasn't|we|we'd|we'll|we're|we've|were|weren't|what|what's|when|when's|where|where's|which|while|who|who's|whom|why|why's|with|won't|would|wouldn't|you|you'd|you'll|you're|you've|your|yours|yourself|yourselves]$"
    // scalastyle:on line.size.limit

    def tokenize(email: String): Seq[String] = {
        val emailBody = email.split("\n").tail.mkString("\n")
        val doc = new Document(emailBody)
        val morph = new Morphology
        val properties = new Properties
        properties.setProperty("tokenize.whitespace", "true")
        doc.sentences(properties).flatMap(_.words).map(morph.stem(_).replaceAll(stopwords, "")).flatMap {
            case "" => None
            case str => Some(str)
        }
    }

}
