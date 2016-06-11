TIPSem: TIPSem (full-featured) and TIPSemB (BASELINE based on Morphosyntax)
--------------------------------------------------------------------------------
website: http://gplsi.dlsi.ua.es/demos/TIMEE/

Conditions/License
------------------
TIPSem and TIPSemB (also known as TIMEE) are  only released for educational / 
research purposes (non-comercial). The use of this software also requires the
acceptance of the licenses of the TreeTagger software. Furthermore, for Spanish,
FreeLing software has to be downloaded and installed under its license. For any 
other use or any question contact Hector Llorens (hllorens@ua.es), University of
 Alicante.

For citing TIPSem/TIPSemB you must include this reference:

@InProceedings{llorens-saquete-navarro:2010:SemEval,
 author    = {Llorens, Hector  and  Saquete, Estela  and  Navarro, Borja},
 title     = {TIPSem (English and Spanish): Evaluating CRFs and Semantic Roles 
              in TempEval-2},
 booktitle = {Proceedings of the 5th International Workshop on Semantic
		Evaluation},
 month     = {July},
 year      = {2010},
 address   = {Uppsala, Sweden},
 publisher = {Association for Computational Linguistics},
 pages     = {284--291},
 url       = {http://www.aclweb.org/anthology/S10-1063}
}



Description
-----------

TIPSem is a temporal information extraction software written in Java.
It automatically annotates from raw text TimeML (http://timeml.org) temporal 
expressions, events, and temporal relations (tlink).
TIPSem is a command line application.


Requirements:
------------
- UNIX-like shell 		(Linux, Mac OS, or Windows with cygwin)
- JRE 6 or later		(http://www.java.com/)
- CRF++ 0.53 (or later)*	(http://crfpp.sourceforge.net/) 
   [recommended/tested 0.54]	(./configure && make && sudo make install)
				Test it: crf_test -v (from any path)
- Treetagger            	(the new version does not work, use 
				www.cognitionis.com/TreeTagger.tar.gz, that is 
				a copy, the original treetagger license aplies)
				Test it: echo "I went to cinema yesterday." | $HOME/TreeTagger/tree-tagger-english
			NOTE: Put TreeTagger inside program-data (default location).
			Otherwse set TreeTagger in any path and tune 
			program-data/config.properties file.
			By default renamed to example.config.properties

* For compiling CRF++ g++ compiler must be < 4.6.
(e.g., UBUNTU -- two versions of g++ can coexist: sudo apt-get install g++-4.4 
	--> ./configure CXX=g++-4.4 && make && sudo make install)


Optional: Only if you are an expert on installing libraries -------------------
- TinySVM 0.09 (for SVM)	(http://chasen.org/~taku/software/TinySVM/)
- YamCha 0.33 (for SVM)*	(http://chasen.org/~taku/software/yamcha/)
- Freeling2.1 (only for Spanish)(http://nlp.lsi.upc.edu/freeling/)(read license)

* For compiling Yamcha downgrade compiler to 4.1 (./configure CXX=g++-4.1).
	You need to install g++-4.1.
	(e.g., UBUNTU LTS versions: sudo apt-get install g++-4.1)


IMPORTANT NOTE:
	- If you never installed a library in Linux you probably need to export 
		the library path: export LD_LIBRARY_PATH=/usr/local/lib
	- And if you do not want to do it every time you probably want to add 
		it in the files:
			.bashrc	.bash_profile

Installation
------------
1) Install the requirements (JRE7, Maven and CRF++).

2) Extract TIPSem folder in any_location of your computer

3) Run mvn clean package to obtain the target/ distribution, 
	copy it in the desired location (e.g., path_to_TIPSem-x.x.x)
	
4) Install TreeTagger as follows:
	- cd path_to_TIPSem-x.x.x/program-data/TreeTagger
	- If tree-tagger-english bin/ cmd/ are not there -> 
	wget http://www.cognitionis.com/TreeTagger.tar.gz (use this copy)
	- make sure that every file is executable in /bin and /cmd, make sure 
	  that "TreeTagger/tree-tagger-english" have execution permissions
	- chmod a+x program-data/TreeTagger/tree-tagger-english 
          program-data/TreeTagger/bin/* program-data/TreeTagger/cmd/*
	  NOTE: if you do not want to use that path intall TreeTagger 
                in any path and tune program-data/config.properties file

5) OPTIONAL (only needed for es-ES): See es.cfg file of Freeling

 # DictionaryFile=$FREELINGSHARE/es/maco.db # uncomment this line

 DictionaryFile=$FREELINGSHARE/es/mydicc.db # comment this line

But mydicc.db does not exist.

Everything works fine after swapping the comment:
 DictionaryFile=$FREELINGSHARE/es/maco.db
 # DictionaryFile=$FREELINGSHARE/es/mydicc.db

permissions
5) Run TIPSem help:  java -jar "path_to_jar/tipsem-1.0.0.jar" -h



Usage
-----
TIPSem input must be UTF8 (or ASCII). Run with -h to get the following help:

 -a,--action <arg>             	Action/s (annotatecrf, annotate)
 
 -ap,--action_parameters <arg> 	Actions can have comma separated params
				Valid parameters are:
				dct (default: today date)
                                entities (default: timex, event, tlink)
                                inputf (default: plain) options: plain, te3input, tml, isotml
                                
-d,--debug                      Debug mode: Output stack trace (default: off) 

 -h,--help                      Print this help 
 

 -l,--lang <arg>                Language code (en,es) (default  auto-detect) 
 
 -t,--text <arg>                To use text instead of a file (for short texts) 



Examples:
--------

Annotate text: java -jar "path_to_jar/tipsem-1.0.0.jar" -t "I saw you yesterday"

Annotate file: java -jar "path_to_jar/tipsem-1.0.0.jar" file.txt

Annotate folder: java -jar "path_to_jar/tipsem-1.0.0.jar" folder/*.txt

Change dct and annotate only events: java -jar "path_to_jar/tipsem-1.0.0.jar"
       -a annotatecrf -ap dct=1999-09-01,entities=event -t "I saw you yesterday"
       
Force language English and use SVM (normal TIPSemB): java -jar 
"path_to_jar/tipsem-1.0.0.jar" -a annotate -t "I saw you yesterday" -l en
For Spanish: -l es

* for -t input the output is written in the standard output.

* for file/s input the output is written is a file with the same name + ".tml"


Contact info: 
------------
Hector Llorens (hllorens@ua.es), University of Alicante, Spain


Deprecated notes:
-------------
- download "tagger package": 
wget ftp://ftp.ims.uni-stuttgart.de/pub/corpora/tree-tagger-linux-3.2.tar.gz
- tar xvzf tree-tagger-linux-3.2.tar.gz
- download "tagging scripts": 
wget ftp://ftp.ims.uni-stuttgart.de/pub/corpora/tagger-scripts.tar.gz
- tar xvzf tagger-scripts.tar.gz
- cd lib
- download "English parameters": 
wget ftp://ftp.ims.uni-stuttgart.de/pub/corpora/english-par-linux-3.2.bin.gz
- gunzip english-par-linux-3.2.bin.gz
- mv english-par-linux-3.2.bin english.par
- make sure that every file is executable in /bin and /cmd, 
make sure that "TreeTagger/tree-tagger-english" have execution 

