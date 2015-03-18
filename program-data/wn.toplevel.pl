#!/usr/bin/perl -w 
#
#
# Author:         Hector Llorens  - hllorens@dlsi.ua.es
# Last modified:  Feb 2009
# Copyright 2009  Hector Llorens


use strict;
use FindBin;                 	# locate this script
use lib "$FindBin::Bin";  	# use the parent directory
my $script_path="$FindBin::Bin";


sub Usage {
	print "Usage:   \n\twn.toplevel.pl [options] < input    \n\t\t---or---    \n\tinput | wn.toplevel.pl [options]\n";
	print " Options:\n";
	print "\t-h		help message\n";
#	print "\t-f format	use format (opt: [pipes]|plain|xml)\n";
	print "\t-p POS		[n,v,j,r] (default n) (noun,verb,adj,adv)\n";
	print "\t-o ext_name	output in files with extension ext_name\n";
	print "\n\n";
}

#my $format='pipes';
my $output_extension;

my $pos='n';

# TODO: mejor hacer un arbol donde cada entidad sepa cual es su padre...
my @wntoplevel=(['entity'],['physical entity|abstraction|thing']); 
my @wntoplevel_clean=('entity','physical entity|abstraction|other',''); 
#,'act','event','group','phenomenon','possession','psychological_feature','state']);

my %wnHoH = (
    'entity' => {
		'physical entity'   => {
					'object'   => "living thing, artifact, location....",
					'thing'   => "part1, body of water, ...",
					'process'   => "z",
					'substance'   => "z",
				    },
		'abstraction'      => {
					'psychological feature'   => "...",
					'attribute'   => "time,...",
					'group'   => "groups of people, gentilicios...",
					'measure'   => "mesures",
					'relation'   => "part2...",
				    },
		'thing'            => "other things (no hyponims)",
    },
);

my $wnHoH_count = scalar(keys(%wnHoH));



# Check STDIN--------------------------------
if (-t 0){print STDERR "Error: No input found\n\n"; Usage(); exit(1);}
#--------------------------------------------

# Process command line arguments
while((defined $ARGV[0]) && ($ARGV[0] =~ /^-/)) {
	my $option = shift(@ARGV);
	for($option){
		/^-h$/ and do { Usage(); exit(0); };
#		/^-f$/ and do { if(defined $ARGV[0] && ($ARGV[0] =~ /^(pipes|plain|xml)$/)){$format = shift(@ARGV); last;}
#				else{print STDERR "Error: -f option requieres a valid format (pipes|plain|xml)\n\n"; exit(1);} };
		/^-p$/ and do { if(defined $ARGV[0] && ($ARGV[0] =~ /^(n|v|j|r)$/)){$pos = shift(@ARGV); last;}
				else{print STDERR "Error: -p option requieres a valid POS string (n,v,j,r)\n\n"; exit(1);} };
		/^-o$/ and do { if(defined $ARGV[0] && ($ARGV[0] =~ /^[[:alnum:]_\.]+$/)){$output_extension = shift(@ARGV); last;}
				else{print STDERR "Error: -o option requieres a valid extension string\n\n"; exit(1);} };
		/^.*$/ and do {print STDERR "Unknown option $option\n\n";Usage(); exit(1);};
	}
}








if($pos ne 'n' && $pos ne 'v'){
	$pos='n';	
}


while(my $input = <STDIN>){
	chomp($input);
	#print STDERR "wn ($input) -n1 -hype$pos";
	
	my $wnout=`wn $input -n1 -hype$pos`; # 2> err.temp
	my $inputant=$input;
	while($wnout eq ''){
		# try to reduce
		$input=~s{^[^_]+_(.*)}{$1};
		if($input eq '' || $input eq $inputant){last;}
		$wnout=`wn $input -n1 -hype$pos`;
		$inputant=$input;
	}
	if ($wnout ne ''){
		my @topclasses=&find_wn_topclasses($wnout);
		#@topclasses=&map_wn_topclasses(@topclasses);
		my $output='';
		my $startlevel=0;
		if($pos eq 'n'){$startlevel=1;}
		for(my $i=$startlevel;$i<=$#topclasses && $i<5;$i++){
			$topclasses[$i]=~s{[[:blank:]]}{_}g;
			$output.="$topclasses[$i]>";
		}
		#$output_arr=split(/>/,$output);
		if($output eq ''){
			if($pos eq 'n'){
				$output='topnoun';
			}else{
				$output='topverb';
			}
		}
		print "$output\n";
		#print "$topclasses[1]|$topclasses[2]|$topclasses[3]|\n"; # ($input)\n";
		#if ($topclasses[0] ne 'entity'){print STDERR "ERROR NO ENTITY TOP\n"; exit(1);} # all are entities
	}else{#`echo "$input" >> $script_path/nown.txt`;} #exit(1);
		print "no_class\n";
	}

#	my $cleartemp=`rm -rf *.temp`;
}










sub find_wn_topclasses{
	my ($wnout)=@_;
	my @hyperclasses;

	my @wnout_lines=split('\n',$wnout);
	my $wnout_lines_count=$#wnout_lines;
	
	my $n=0;
	while($n <= $wnout_lines_count){
		if( $wnout_lines[$n] =~ m{^[[:blank:]]*=>[[:blank:]]*([^,]*).*$} ){ # keep only the first of the synset (until ,)
			@hyperclasses=($1,@hyperclasses);
		}
		if($wnout_lines[$n] =~ m{^[[:blank:]]*=>[[:blank:]]*(entity).*$}i){last;}
		$n++;
	}

	return @hyperclasses;
}

sub map_wn_topclasses{
	my @hyperclasses=@_;

	
	my $n=0;
	my $hyperclasses_count=$#hyperclasses;
	my %mapping_level=%wnHoH; # to use @wntoplevel

	my @mapped_classes;

	my $current_class='';	

	# mientras haya classes buscar una top mapeada (entity, abstraction,...)
	# si la encontramos la guardamos como $hyperclasses[0] y borramos todas las anteriores
	# hacer así hasta los niveles deseados mapeados

	while($n <= $hyperclasses_count){
		if( $hyperclasses[$n] =~ m{^[[:blank:]]*=>[[:blank:]]*([^,]*).*$} ){	# class found only the first of the synset (until ,)
			$current_class=$1;
			# si esta entre las keys del maping_level actual actualizamos el mapping level al siguiente y mapeamos la clase actual
			# si no se encuentra entre las claves provamos con la siguiente (así hasta acabar con las clases o los niveles)
			@mapped_classes=($1,@mapped_classes);
		}
		$n++;
	}


	return @mapped_classes;
}










