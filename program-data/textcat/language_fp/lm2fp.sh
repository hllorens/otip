#!/bin/bash

IFS="
"

i=0;
for line in $(< $1);do
	i=$(($i +1));
	ngram=`echo $line | sed "s/^\([^[:space:]]*\).*\$/\1/"`
	echo -e "$ngram\t$i"; 
done;
