package nl.vu.ai.lsde.enron


object Commons {

    val SOURCE_ENRON_DATA = "hdfs:///user/hannesm/lsde/enron"

    val LSDE_USER_SPACE = "hdfs:///user/lsde03"
    val LSDE_ENRON = s"$LSDE_USER_SPACE/enron"
    val ENRON_EXTRACTED_TXT = s"$LSDE_ENRON/extracted_txt"

}
