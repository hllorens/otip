TIPSem: TIPSem (full-featured) and TIPSemB (BASELINE based on Morphosyntax)
--------------------------------------------------------------------------------
website: http://www.cognitionis.com

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
- UNIX-like shell 		(Linux, Mac OS) (Windows + cygwing not recommended, difficult to make it work)
- JRE 6 or later		(http://www.java.com/)
- Maven                         (http://maven.apache.org/) 
   [last tested 3.3.9]		just download untar and add bin/ to the path
- gcc/g++                       (http://openwall.info/wiki/internal/gcc-local-build) 
   [last tested 4.6.3]          to compile CRF++ , newer CRF++ can work with newer g++
- CRF++ 0.53 (or later)*	(https://taku910.github.io/crfpp/) 
   [recommended/tested 0.58]	./configure --prefix=$HOME/local && make && make install, add bin/ to the path
				Test it: crf_test --version (from any path)
- Treetagger            	The new version does not work, use 
				www.cognitionis.com/TreeTagger.tar.gz or https://www.dropbox.com/s/4pdlhg4rwv6smos/TreeTagger.tar.gz?dl=0, that is a copy [the original treetagger license applies])


* For compiling CRF++ if 0.58 you can use g++ >=4.6 < 0.55, g++ compiler must be <= 4.5.
(e.g., UBUNTU -- two versions of g++ can coexist: sudo apt-get install g++-4.4 
	--> ./configure CXX=g++-4.4 --prefix $HOME/local && make && make install)


Optional: Only if you are an expert on installing libraries -------------------
- TinySVM 0.09 (for SVM)	(http://chasen.org/~taku/software/TinySVM/)
- YamCha 0.33 (for SVM)*	(http://chasen.org/~taku/software/yamcha/)
- Freeling2.1 or greater (only for Spanish)(http://nlp.lsi.upc.edu/freeling/)(read license)
  - Tested successfully up to version 4.0 (June-2019)
    - IMPORTANT: We don't provide CRF models for tlinks in es-es so use SVM for that. See known-issues and solution.

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
1) Install the requirements (JRE7, Maven and CRF++). On windows you can just download zip, extract to C: and add to the PATH

2) Extract TIPSem folder in any_location of your computer

3) Run: "mvn clean package" to obtain the "target/" distribution, 
	copy it in the desired location (e.g., path_to_TIPSem-x.x.x)
	
4) Install TreeTagger as follows:
	- cd path_to_TIPSem-x.x.x/program-data/ (the compiled "target/" one)
	- If TreeTagger/tree-tagger-english bin/ cmd/ are not there -> 
	wget http://www.cognitionis.com/TreeTagger.tar.gz or https://www.dropbox.com/s/4pdlhg4rwv6smos/TreeTagger.tar.gz?dl=0
	- tar xfzf TreeTagger.tar.gz
	- make sure that every file is executable in /bin and /cmd, make sure 
	  that "TreeTagger/tree-tagger-english" have execution permissions
	- chmod a+x program-data/TreeTagger/tree-tagger-english 
          program-data/TreeTagger/bin/* program-data/TreeTagger/cmd/*
	  NOTE: if you do not want to use that path intall TreeTagger 
                in any path and tune program-data/config.properties file
		By default renamed to example.config.properties
	Test it: echo "I went to cinema yesterday." | path-to-otip/program-data/TreeTagger/tree-tagger-english
		WINDOWS/CIGWIN: Download Windows version of TreeTagger https://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/#Windows
		perl cmd/utf8-tokenize.perl -e -a lib/english-abbreviations FILE-TO-ANNOTATE | ./bin/tree-tagger lib/english.par -token -lemma -sgml -no-unknown

Modification to make TreeTagger work alone in tree-tagger-english script (with relative paths):
```
#!/bin/bash

# Hector Llorens hack for executing TreeTagger

scriptPath=$(cd $(dirname $0); pwd -P)

pushd . > /dev/null;

cd $scriptPath;

BIN=./bin
CMD=cmd
LIB=lib

OPTIONS="-token -lemma -pt-with-lemma -no-unknown"

TOKENIZER=${CMD}/tokenize.pl
TAGGER=${BIN}/tree-tagger
ABBR_LIST=${LIB}/english-abbreviations
PARFILE=${LIB}/english.par
LEXFILE=${LIB}/english-lexicon.txt

#$TOKENIZER -e -a $ABBR_LIST $* |
# remove empty lines
#grep -v '^$' |
# external lexicon lookup
#perl $CMD/lookup.perl $LEXFILE |
# tagging
#$TAGGER $OPTIONS $PARFILE | 
#perl -pe 's/\tV[BDHV]/\tVB/;s/IN\/that/\tIN/;'
##TOKENIZER=${CMD}/utf8-tokenize.perl

perl cmd/utf8-tokenize.perl -e -a lib/english-abbreviations $* | ./bin/tree-tagger lib/english.par -token -lemma -sgml -no-unknown
#perl ${CMD}/utf8-tokenize.perl -e -a $ABBR_LIST $* | ${BIN}/tree-tagger $ABBR_LIST -token -lemma -sgml -no-unknown | 
#perl -pe 's/\tV[BDHV]/\tVB/;s/IN\/that/\tIN/;'

popd;
```
	CIGWIN issue: TIPSem might be trying bad paths i.e., non windows compatible or too compatible? java.io.IOException: Cannot run program "/bin/sh": CreateProcess error=2, The system cannot find the file specified. Tree Tagger works standalone with the script above but not when called from TIPSem in cygwin. 
	SOLUTION: cygpath -w should be added in many places, currently the program is trying file:// which is not working on cygwin



5) Obtain models:

- Option A (easy): Copying the models from http://cognitionis.com/TIPSem.zip or https://www.dropbox.com/s/wf6pfw3ud9jclag/TIPSem.zip?dl=0. They are located in the folders "program-data/CRF++" and "program-data/SVM".
    -- copy the "models/" folder into your "target/program-data/CRF++/models/"

- Option B (difficult): Training your own models using a TimeML annotated corpus e.g., TimeBank or the TempEval-3 trainset.
This should provide a good insight on the actions available including training+test over a dataset:
https://github.com/hllorens/otip/blob/master/src/main/java/com/cognitionis/tipsem/OptionHandler.java
Running the help option (see below) will also provide useful info.

6) OPTIONAL (only needed for es-ES): See es.cfg file of Freeling

 # DictionaryFile=$FREELINGSHARE/es/maco.db # uncomment this line

 DictionaryFile=$FREELINGSHARE/es/mydicc.db # comment this line

But mydicc.db does not exist.

Everything works fine after swapping the comment:
 DictionaryFile=$FREELINGSHARE/es/maco.db
 # DictionaryFile=$FREELINGSHARE/es/mydicc.db

permissions are important



Usage
-----

IMPORTANT: Make sure you have models (installation step 5)

Run TIPSem help:  java -jar "path_to_jar/tipsem-1.0.0.jar" -h

TIPSem input must be UTF8 (or ASCII). Run with -h to get the following help:

 -a,--action <arg>             	Action/s (annotatecrf, annotate)
 
 -ap,--action_parameters <arg> 	Actions can have comma separated params
				Valid parameters are:
				dct (default: today date)
                                entities (default: timex, event, tlink) [separated by ; e.g., timex;event]
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

Change annotate timex and events but not tlinks: java -jar "path_to_jar/tipsem-1.0.0.jar"
       -a annotatecrf -ap entities=timex;event -t "I saw you yesterday"
       
Force language English and use SVM (normal TIPSemB): java -jar 
"path_to_jar/tipsem-1.0.0.jar" -a annotate -t "I saw you yesterday" -l en
For Spanish: -l es

* for -t input the output is written in the standard output.

* for file/s input the output is written is a file with the same name + ".tml"


Contact info: 
------------
Hector Llorens (hector.llorens.martinez@gmail.com)


Known issues:
------------
https://github.com/hllorens/otip/issues/2 There are no crf models for es-es tlink e-dct and e-t (tlink)
Errors found (CRF++):
        java.lang.Exception: Template file (TIPSemB_categ_e-t_ES.CRFmodel) not found.
        java.lang.Exception: Template file (TIPSemB_categ_e-dct_ES.CRFmodel) not found.
Solution use the SVM version in general for es-es with `-a annotate`.
If you need to annotate timex and events with CRF and only links with SVM then make use of `-ap entities=tlink -inputf tml` using the timex and event annotated part which you can obtain before with `-ap entities=timex;event -inputf plain`.
	

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

