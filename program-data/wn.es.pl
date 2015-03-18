#!/usr/bin/perl -w 
#
# WN es api
#
# Author:         Hector Llorens  - hllorens@dlsi.ua.es
# Last modified:  Apr 2009
# Copyright 2009  Hector Llorens


use strict;
use FindBin;                 # locate this script
use lib "$FindBin::Bin";  # use the parent directory
my $script_path="$FindBin::Bin";


sub Usage {
	print "Usage:   \n\twn.es.pl word [options]\n\t\t---or---    \n\tword | wn.es.pl [options]\n";
	print " Options:\n";
	print "\t-h		help message\n";
	print "\tAction:";
	print "\t\t-synsn		print sinonims\n";
	print "\t\t-hypen		print hyperonimy\n";
	print "\t\t-hypon		print hyponims tree\n";
	print "\n";
	print "\tSense:";
	print "\t-n number		display only a concrete sense (default 1)\n";
	print "\n";
	print "\tFilter:";
	print "\t-match word		match word in action\n";
	print "\n\n";
}


my $word;
my $action='synsn';
#DEPRECATED: my $wn_path="$script_path/wn_es_nouns.utf8.ewn";
#my $wn_path="$script_path/wn_es_nouns.ewn";
my $wn_path="/home/hector/Dropbox/WordNets/WN_EWN_ES/wn_es_nouns.ewn";
my $match;
my $found=0;
my $plural=0;
my $max_recursivity=12;
my $recursivity=0;
my $onlysense=0;

# Process input
if((defined $ARGV[0]) && !($ARGV[0] =~ /^-/)) {
	$word=shift(@ARGV);
}else{ # Check STDIN
	if (! -t 0){while(<STDIN>){$word.=$_;}}
	else{print STDERR "Error: No input found\n\n"; exit(1);}
}

while((defined $ARGV[0]) && ($ARGV[0] =~ /^-/)) {
	my $option = shift(@ARGV);
	for($option){
		/^-h$/ and do { Usage(); exit(1); };
		/^-match$/ and do { if(defined $ARGV[0]){$match = shift(@ARGV); last;}
				else{print STDERR "Error: -match requieres a word\n\n"; exit(1);} };
		/^-n$/ and do { if(defined $ARGV[0]){$onlysense = shift(@ARGV); last;}
				else{print STDERR "Error: -s requieres a sense number\n\n"; exit(1);} };
		/^-synsn$/ and do { $action='synsn'; last;};
		/^-hypen$/ and do { $action='hypen'; last;};
		/^-hypon$/ and do { $action='hypon'; last;};
		/^.*$/ and do {print STDERR "Unknown option $option\n\n";Usage(); exit(1);};
	}
}


&main(); print "\n";


sub main{
	print STDERR "\n-------------------------------\n- $action for \"$word\"\n-------------------------------\n\n";
	open (FILEID, $wn_path);
	#$/ = "\r\n\r\n"; # read line by line. Other solution read paragraphs \n\n
	$/ = "\n\n"; # read line by line. Other solution read paragraphs \n\n
	while (<FILEID>) {
		my $text=$_;
		if($text=~m{2\sLITERAL\s\"$word\"\n\s*3\sSENSE\s(\d+)\n}is){
			my $sense=$1;
			#print STDERR "$sense $onlysense\n";
			if($onlysense==0 || $sense eq $onlysense){
				$recursivity=0;
				$found++;
				print "$word - sense=$sense\n";
				my $wn_element={};
				&parse_wn_element($wn_element,$text,$word);
				#last;
				for("$action"){
					/^synsn$/ and do {
						&print_synsn($wn_element,$word,$sense);
						last;
						};
					/^hypen$/ and do {
						&print_hyp($wn_element,$word,$sense,"hyperonym");
						last;
						};
					/^hypon$/ and do {
						&print_hyp($wn_element,$word,$sense,"hyponym");
						last;
						};

					/^.*$/ and do {print STDERR "Unknown action $action\n\n";Usage(); exit(1);};
				}
	#			print "\nprinting hash-------------------------------------\n";
	#			&print_hash($wn_element,0);
			}
		}
	}
	close(FILEID);
	if(!$found && $word=~m{s$} && $plural==0 && length($word) > 3){
		$word=~s{s$}{};
		$plural=1;
		&main();
	}
	if(!$found && $plural==1){
		$word=~s{.$}{};
		$plural=2;
		&main();
	}
}




sub parse_wn_element(){
	my ($wn_element,$wn_text,$word)=@_;
	my @parents=();
	my $wn_element_pointer=$wn_element;
	my $level=-1;
	my $tag='';
	my $value='';

	my @lines=split("\n",$wn_text);
#	$wn_element_pointer=$wn_element->{"$word"};
	foreach my $line ( @lines ){
		my $prelevel=$level;
		my $pretag=$tag;
		$tag='';$value='';
		$line=~s/^[[:blank:]]*([^[:blank:]]+)\s+([^[:blank:]]+)(?:\s+([^[:blank:]]+))?[[:blank:]]*$/$1/;
		$level=$1; $tag=$2; if(defined($3)){$value=$3;}
		if($level == 0){$tag="$word"; $value='';}
#		print "Level: $level - Tag= $tag - Value=$value\n";
		if($value ne ''){$tag="$tag=$value";}

		if($level > $prelevel){
			if($level > 0){ $wn_element_pointer=$wn_element_pointer->{"$pretag"}; }
			push(@parents,$wn_element_pointer);
			$wn_element_pointer->{"$tag"}= {} unless(exists($wn_element_pointer->{"$tag"}));
#			print "changed level to $level\n"
		}else{
			if($level == $prelevel){
				pop(@parents);
				$wn_element_pointer->{"$tag"}= {} unless(exists($wn_element_pointer->{"$tag"}));
				push(@parents,$wn_element_pointer);
			}else{
				for(my $i=$prelevel;$i>$level;$i--){
					pop(@parents);					
				}
				$wn_element_pointer=$parents[$#parents];
				pop(@parents);
				$wn_element_pointer->{"$tag"}= {} unless(exists($wn_element_pointer->{"$tag"}));
				push(@parents,$wn_element_pointer);				
			}
		}
	}


	return $wn_element_pointer;
}



sub print_hash{
	my ($hash_ref,$level)=@_; my %hash=%$hash_ref;
	for my $key ( keys %hash ) {
		my $deref=$hash{$key};
		for (my $tab=0; $tab < $level; $tab++){print "\t";} # tabbing
		if (ref($deref) eq "HASH") { # if is a hash explore
			print "$key\n"; # key
			&print_hash($deref,$level+1); # sub keys recusrively
		}else{ # else print value
			print "$key => $hash{$key}\n";			
		}
	}
}




sub print_synsn{
	my ($wn_element,$word,$sense)=@_;
	my $variants=$wn_element->{$word}{"VARIANTS"};
	print "=> ";
	for my $key ( keys %$variants ) {
		$key=~s{LITERAL=\"([^\"]+)\"}{$1}i;
		print "$key, ";
	}
	print "\n";
}



sub print_hyp{
	my ($wn_element,$word,$sense,$direction)=@_;
	my $hyperonims=$wn_element->{$word}{"INTERNAL_LINKS"}{"RELATION=\"has_$direction\""}{"TARGET_CONCEPT"};
	#print "hypen $word-$sense\n";
	for my $key ( keys %$hyperonims ) {
		if($key=~m{LITERAL=\"([^\"]+)\"}){
			my $senseref=$wn_element->{$word}{"INTERNAL_LINKS"}{"RELATION=\"has_$direction\""}{"TARGET_CONCEPT"}{"$key"};
			my ($sense,$senseval) = each (%$senseref);
			$sense=~s{SENSE=}{};
			$key=~s{LITERAL=\"([^\"]+)\"}{$1};
			&recursive_hypen($key,$sense,$direction);
			$recursivity--;
		}
	}
}

# TODO tener en cuenta el sentido
sub recursive_hypen{
	my ($word,$sense,$direction)=@_;
	my $text;
	my $hypen_found=0;
	if($recursivity>$max_recursivity){print STDERR "Error: Hypen recursivity stack overrided\n"; exit(0);}
	$recursivity++;
	open (FILEID2, $wn_path);
	$/ = "\n\n"; # read line by line. Other solution read paragraphs \n\n
	while (<FILEID2>) {
		$text=$_;
		if($text=~m{2\sLITERAL\s\"$word\"\n\s*3\sSENSE\s$sense\n}is){
			$hypen_found=1;
			last;
		}
	}
	close(FILEID2);
	if(!$hypen_found){print STDERR "Error: Hypen not found\n"; exit(0);}
	my $wn_element={};
	&parse_wn_element($wn_element,$text,$word);
	for (my $tab=0; $tab < $recursivity; $tab++){print "\t";} # tabbing
	&print_synsn($wn_element,$word,$sense);
	&print_hyp($wn_element,$word,$sense,$direction);
}

