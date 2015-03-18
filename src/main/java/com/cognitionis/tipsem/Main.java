package com.cognitionis.tipsem;

import java.io.*;
import java.text.*;
import java.util.*;
import org.apache.commons.cli.*;
import com.cognitionis.utils_basickit.StringUtils;

/** @author Hector Llorens 
 *  @since 2009
 */
public class Main {

    /** @param args the command line arguments */
    public static void main(String[] args) {
        try {
            String lang = null;
            String action = "annotatecrf"; //default action
            String action_parameters = null;
            String input_files[];
            String input_text = null;
            long startTime = System.currentTimeMillis();
            //System.out.println("Current Date Time : " + dateFormat.format(ExecTime));


            Options opt = new Options();
            //addOption(String opt, boolean hasArg, String description)
            opt.addOption("h", "help", false, "Print this help");
            opt.addOption("l", "lang", true, "Language code (default \"EN\" [English])");
//            opt.addOption("i", "input_format", true, "Input format (default autodetect, timebank, ancora, pipes, unknownxml, unknownbrakets)");
//            opt.addOption("ie", "input_encoding", true, "Input encoding (default autodetect, ASCII, UTF-8, ISO)");
//            opt.addOption("ve", "valid_encodings", true, "Valid encodings [default UTF-8] )");
            opt.addOption("a", "action", true, "Action/s to be done (annotate,TAn)");
            opt.addOption("ap", "action_parameters", true, "Optionally actions can have parameters (-a annotate -ap approach=TIPSemB,dct=1999-09-01,entities=event)");
            opt.addOption("t", "text", true, "To use text instead of a file (for short texts)");
//            opt.addOption("o", "output_extension", true, "Output extension (default null - STDOUT, action.xml)");
            opt.addOption("d", "debug", false, "Debug mode: Output errors stack trace (default: disabled)");

            PosixParser parser = new PosixParser();
            CommandLine cl_options = parser.parse(opt, args);
            input_files = cl_options.getArgs();
            HelpFormatter hf = new HelpFormatter();
            if (cl_options.hasOption('h')) {
                hf.printHelp("TIPSem", opt);
                System.exit(0);
            } else {
                if (cl_options.hasOption('d')) {
                    System.setProperty("DEBUG", "true");
                }
                if (cl_options.hasOption('l')) {
                    lang = cl_options.getOptionValue('l').toLowerCase();
                    if (lang.length() != 2) {
                        hf.printHelp("TIPSem", opt);
                        throw new Exception("Error: incorrect language " + lang + " -- must be 2 chars");
                    }
                }
                if (cl_options.hasOption('a')) {
                    action = cl_options.getOptionValue("a");
                    try {
                        OptionHandler.Action.valueOf(action.toUpperCase());
                    } catch (Exception e) {
                        String errortext = "\nValid acctions are:\n";
                        for (OptionHandler.Action c : OptionHandler.Action.values()) {
                            errortext += "\t" + c.name() + "\n";
                        }
                        throw new RuntimeException("\tIlegal action: " + action.toUpperCase() + "\n" + errortext);
                    }
                } /*else {
                action = "annotate";
                }*/
                if (cl_options.hasOption("ap")) {
                    action_parameters = cl_options.getOptionValue("ap");
                }

                if (cl_options.hasOption("t")) {
                    input_text = cl_options.getOptionValue("t");
                }

            }
            // Convert input text to a file if necessary
            if (input_text != null && input_text.length() > 0) {
                System.err.println("TIPSem text: " + input_text);
                // Save text to a default file
                //String tmpfile = FileUtils.getApplicationPath() + "program-data/tmp/tmp" + dateFormat.format(ExecTime);
                final DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SSS");
                String tmpfile = "tmp" + dateFormat.format(new Date());
                BufferedWriter outfile = new BufferedWriter(new FileWriter(tmpfile));
                try {
                    outfile.write(input_text + "\n");
                } finally {

                    if (outfile != null) {
                        outfile.close();
                    }
                    input_files = new String[1];
                    input_files[0] = tmpfile;
                }
            }
            OptionHandler.doAction(action, input_files, action_parameters, lang);
            long endTime = System.currentTimeMillis();
            long sec=(endTime-startTime)/1000;
            if(sec<60){
                System.err.println("Done in "+StringUtils.twoDecPosS(sec)+" sec!\n");
            }else{
                System.err.println("Done in "+StringUtils.twoDecPosS(sec/60)+" min!\n");
            }
            if (input_text != null) {
                System.err.println("Result:\n");
                BufferedReader reader = new BufferedReader(new FileReader(input_files[0] + ".tml"));
                try{
                String text = null;
                while ((text = reader.readLine()) != null) {
                    System.out.println(text + "\n");
                }
                }finally{
                    if(reader!=null) reader.close();
                }
            }
        } catch (Exception e) {
            System.err.println("Errors found:\n\t" + e.getMessage() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
            }
            System.exit(1);
        }
    }
}
