package nl.vu.ai.lsde.enron.spam


object SpamTrainerTest extends App {

    // scalastyle:off line.size.limit
    val tests = Seq(
        "Subject: duns number changes\nfyi\n- - - - - - - - - - - - - - - - - - - - - - forwarded by gary l payne / hou / ect on 12 / 14 / 99 02 : 35 pm\n- - - - - - - - - - - - - - - - - - - - - - - - - - -\nfrom : antoine v pierre 12 / 14 / 99 02 : 34 pm\nto : tommy j yanowski / hou / ect @ ect , kathryn bussell / hou / ect @ ect , gary l\npayne / hou / ect @ ect , diane e niestrath / hou / ect @ ect , romeo d ' souza / hou / ect @ ect ,\nmichael eiben / hou / ect @ ect , clem cernosek / hou / ect @ ect , scotty\ngilbert / hou / ect @ ect , dave nommensen / hou / ect @ ect , david rohan / hou / ect @ ect ,\nkevin heal / cal / ect @ ect , richard pinion / hou / ect @ ect\ncc : mary g gosnell / hou / ect @ ect , jason moore / hou / ect @ ect , samuel\nschott / hou / ect @ ect , bernice rodriguez / hou / ect @ ect\nsubject : duns number changes\ni will be making these changes at 11 : 00 am on wednesday december 15 .\nif you do not agree or have a problem with the dnb number change please\nnotify me , otherwise i will make the change as scheduled .\ndunns number change :\ncounterparty cp id number\nfrom to\ncinergy resources inc . 62163 869279893 928976257\nenergy dynamics management , inc . 69545 825854664 088889774\nsouth jersey resources group llc 52109 789118270 036474336\ntransalta energy marketing ( us ) inc . 62413 252050406 255326837\nphiladelphia gas works 33282 148415904 146907159\nthanks ,\nrennie\n3 - 7578",
        "Subject: pennzenergy property details\n- - - - - - - - - - - - - - - - - - - - - - forwarded by ami chokshi / corp / enron on 12 / 17 / 99 04 : 03\npm - - - - - - - - - - - - - - - - - - - - - - - - - - -\ndscottl @ . com on 12 / 14 / 99 10 : 56 : 01 am\nto : ami chokshi / corp / enron @ enron\ncc :\nsubject : pennzenergy property details\nami , attached is some more details on the devon south texas properties . let\nme\nknow if you have any questions .\ndavid\n- devon stx . xls",
        "Subject: smoke 9885\ncgfsaw 91 cmdaawlolmrlbw 9 rcmlob 3 muz 3 i =\ngary crew has used kim as one of the main characters to tell the story about the wild children . billy ' s papa tries to tell him that it is all for the best\nfirst confucianism deals with the rational cosmic order and the organization of worldly affairs . four modified varieties of this last \" ( twain 8 ) .",
        "Subject: diet medications online\nstop wasting money on prescription drugs . get them online for 80 % off .\nvlagra , clalis , zyban , prozac , xenlcal , and many many more . . .\nstop paying more than you have too !\n- todays special -\nviagra , retail price $ 15 . 99 , our price $ 2 . 99\ncialis , retail price $ 17 . 99 , our price $ 3 . 99\nshipped world wide\nno prescription required\nvisit us here : http : / / m 3 dz . com / ? zodiac\nunsubscribe here : http : / / m 3 dz . com / rm . php ? zodiac"
    )
    // scalastyle:on line.size.limit

    tests map Tokenizer.tokenize foreach println

}
