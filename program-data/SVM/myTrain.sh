#!/bin/bash
scriptPath=$(cd $(dirname $0); pwd -P)
currentPath=$(pwd -P)
#SVM_PARAM="-t 1 -d 2 -c 1";
#export SVM_PARAM;
# $1=templatefile $2=data $3=model

if [ $# != 3 ];then
echo "3 params required: featuresFile trainCorpus modelname"
exit 0;
fi

template=`head -n 1 $1`

if [ `echo "$template" | grep "^[[:blank:]]*F.*:.*:.*[0-9][[:blank:]]*$" | wc -w` -eq 0  ];then
	echo "Incorrect pattern template: $template";
fi

if [ `echo "$template" | grep "[^-+.,FT:0-9[:blank:]]" | wc -w` -gt 0  ];then
	echo "Incorrect pattern template: $template";
fi


cd $scriptPath; make CORPUS=$2 FEATURE="$template" MODEL=model train

#/usr/local/bin/yamcha  -F"$template" -o $scriptPath/model.data $2
#/usr/bin/perl -w /usr/local/libexec/yamcha/mkparam   $scriptPath/model < $scriptPath/model.data
#/usr/bin/perl -w /usr/local/libexec/yamcha/mksvmdata $scriptPath/model
#rm -f $scriptPath/model.data
#sort < $scriptPath/model.svmdata | uniq | sort > $scriptPath/model.svmdata.sort
#mv -f $scriptPath/model.svmdata.sort $scriptPath/model.svmdata

#echo "Training..."
#/usr/bin/perl -w /usr/local/libexec/yamcha/svm_learn_wrapper -t1  -s /usr/local/bin/svm_learn -o "-t 1 -d 2 -c 1" $scriptPath/model.svmdata $scriptPath/model.svmmodel 2>&1 | tee $scriptPath/model.log

#echo "Formatting model..."
#/usr/bin/perl -w /usr/local/libexec/yamcha/zipmodel $scriptPath/model.param $scriptPath/model.svmmodel > $scriptPath/model.txtmodel
#/bin/gzip -f $scriptPath/model.txtmodel
#rm -f $scriptPath/model.param $scriptPath/model.svmmodel

#/bin/gzip -dc $scriptPath/model.txtmodel.gz | /usr/bin/perl -w /usr/local/libexec/yamcha/showse > $scriptPath/model.se

#/usr/bin/perl -w /usr/local/libexec/yamcha/mkmodel -t /usr/local/libexec/yamcha $scriptPath/model.txtmodel.gz $3

mv $scriptPath/model.model $3
rm -f $scriptPath/model.*
cd $currentPath # can be omitted

