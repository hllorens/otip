#
#  $Id: Makefile.in,v 1.12 2004/09/20 09:59:16 taku-ku Exp $;
#
prefix     = /usr/local
TOOLDIR    = /usr/local/libexec/yamcha
SVM_PARAM  = -t 1 -d 1 -c 1 -m 1024  # t type of kenel (default 1=polynomial) d=polynomaial degree (default 1) c=error margin (Default 1)
#FEATURE    = F:-2..2:0.. T:-2..-1
FEATURE    = F:-2..2:0..1 F:0:2.. T:-2..-1
DIRECTION  =

# 1 MEANS pair-wise ONE VS ONE (DEFAULT), 2 MEANS ONE VS REST
MULTI_CLASS = 2
DESTDIR = 

# data
CORPUS = 
MODEL  =

# misc tools
SVM_LEARN = /usr/local/bin/svm_learn
PERL      = /usr/bin/perl -w
GZIP      = /bin/gzip
YAMCHA    = /usr/local/bin/yamcha
RM        = rm -f
SORT      = sort
UNIQ      = uniq

# Do not modify 
all:
	echo "Usage: make CORPUS=Your-Corpus-File MODEL=Model-Prefix-Name train"

train: select convert learn zip se compile

select:
	$(YAMCHA) $(DIRECTION) -F'$(FEATURE)' -o $(MODEL).data $(CORPUS) 

convert:
	$(PERL) $(TOOLDIR)/mkparam   $(MODEL) < $(MODEL).data
	$(PERL) $(TOOLDIR)/mksvmdata $(MODEL)
	$(RM) $(MODEL).data
	$(SORT) < $(MODEL).svmdata | $(UNIQ) | $(SORT) > $(MODEL).svmdata.sort
	mv -f $(MODEL).svmdata.sort $(MODEL).svmdata

learn:
	$(PERL) $(TOOLDIR)/svm_learn_wrapper -t$(MULTI_CLASS) -s $(SVM_LEARN) -o "$(SVM_PARAM)" $(MODEL).svmdata $(MODEL).svmmodel 2>&1 | tee $(MODEL).log

zip:
	$(PERL) $(TOOLDIR)/zipmodel $(MODEL).param $(MODEL).svmmodel > $(MODEL).txtmodel
	$(GZIP) -f $(MODEL).txtmodel
	$(RM) $(MODEL).param $(MODEL).svmmodel

se:	
	$(GZIP) -dc $(MODEL).txtmodel.gz | $(PERL) $(TOOLDIR)/showse > $(MODEL).se

compile:
	$(PERL) $(TOOLDIR)/mkmodel -t $(TOOLDIR) $(MODEL).txtmodel.gz $(MODEL).model

check:
clean:
distclean:
distdir:
uninstall:
	rm -f $(DESTDIR)$(prefix)/libexec/yamcha/Makefile
