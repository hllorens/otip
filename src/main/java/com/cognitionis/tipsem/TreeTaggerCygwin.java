package com.cognitionis.tipsem;

import java.io.*;
import java.util.*;
import com.cognitionis.utils_basickit.FileUtils;






// EXPERIMENT TO MAKE JAVA/CYGWIN compatible because of PATHS and avoid inline scripts...












/**
 *
 * @author Hector Llorens
 * @since 2011
 */
public class TreeTaggerCygwin {


    //private static String program_path = FileUtils.getApplicationPath() + "program-data/TreeTagger/";
    private static String program_path = get_path();
    private static String program_bin = program_path + "bin"+File.separator+"tree-tagger";
    private static String program_bin_tokenizer = program_path + "tree-tagger-english";
    private static String program_model = program_path + "lib"+File.separator+"english.par"; // par stands for parameter file



    private static String get_path(){
        Properties prop = new Properties();
        String ret=null;
        try{
            prop.load(new FileInputStream(FileUtils.getApplicationPath() + "program-data"+File.separator+"config.properties"));
            ret=prop.getProperty("treetagerpath");
            if(ret==null){
                ret=FileUtils.getApplicationPath() + "program-data"+File.separator+"TreeTagger"+File.separator;
            }
        }catch(Exception e){
            ret=FileUtils.getApplicationPath() + "program-data"+File.separator+"TreeTagger"+File.separator;
        }
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
            System.out.println("Usig Treetagger at: " + ret);
        }
        return ret;
    }

    /**
     * Runs TreeTagger over a one-col PipesFile of Tokens (one token per line)
     * and saves the output in a .treetag file (PipesFile)
     *
     * Format word|pos|lemma
     *
     * NOTE: You can merge the information obtained in the desired column using MERGE_PIPES action
     *
     * @param filename
     * @return Output filename
     */
    public static String run(String filename) {
        System.out.println("\n\nFile: " + filename);
        execute(filename, filename + ".treetag");
        return filename + ".treetag";
    }

    public static String run2(String filename) {
        System.out.println("\n\nFile: " + filename);
        execute(filename, filename + ".tml");
        return filename + ".tml";
    }


    /**
     * Runs TreeTagger over a multi-col PipesFile using tok_col rows as tokens
     * and saves the output in a .treetag file (PipesFile)
     *
     * Format word|pos|lemma
     *
     * NOTE: You can merge the information obtained in the desired column using MERGE_PIPES action
     *
     * @param filename
     * @param tok_col
     * @return Output filename
     */
    public static String run(String filename, int tok_col) {
        try {
            String line;
            BufferedReader pipesreader = new BufferedReader(new FileReader(filename));
            File temptokfile = new File(filename + ".tok");
            BufferedWriter tokwriter = new BufferedWriter(new FileWriter(temptokfile));

            try {
                while ((line = pipesreader.readLine()) != null) {
                        String[] linearr = line.split("\\|");
                        if (linearr.length > tok_col) {
                            tokwriter.write(linearr[tok_col] + "\n");
                        }
                     else {
                        tokwriter.write("\n");
                    }

                }
            } finally {
                if (pipesreader != null) {
                    pipesreader.close();
                }
                if (tokwriter != null) {
                    tokwriter.close();
                }
            }

            execute(filename + ".tok", filename + ".treetag");
            temptokfile.delete();
            temptokfile=null;
        } catch (Exception e) {
            System.err.println("Errors found (TreeTagger):\n\t" + e.toString());
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
        return filename + ".treetag";

    }

    private static void execute(String tokensfile, String outputfile) {
        try {
            System.out.println("\n\nFile: " + tokensfile);
            String[] command = {program_bin, "-token", "-lemma", program_model, tokensfile};
            Process p = Runtime.getRuntime().exec(command);

            // TreeTagger Format
            // token  POS lemma (separated by \t)

            String intokens_line;
            String treetag_line;
            System.setProperty("file.separator","/");
            System.out.println("\n\nFile: " + tokensfile);
            BufferedReader intokens_reader = new BufferedReader(new FileReader(tokensfile));
            BufferedWriter output = new BufferedWriter(new FileWriter(outputfile));
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try {
                while ((intokens_line = intokens_reader.readLine()) != null) {
                    if (intokens_line.length() != 0) {
                        if ((treetag_line = stdInput.readLine()) != null) {
                            String[] treetag_arr = treetag_line.replaceAll("\\|", "-").split("\t");
                            if (treetag_arr.length == 3) {
                                if (treetag_arr[0].trim().equals(intokens_line.trim())) {
                                    output.write(treetag_arr[0] + "|" + treetag_arr[1] + "|" + treetag_arr[2] + "\n");
                                } else {
                                    throw new Exception("Unexpected TreeTagger output token:" + treetag_arr[0].trim() + " - expected: " + intokens_line.trim());
                                }
                            } else {
                                throw new Exception("Malformed TreeTagger output:" + treetag_line);
                            }
                        } else {
                            throw new Exception("Unexpected end of TreeTagger output: tokens=" + intokens_line);
                        }
                    } else {
                        output.write("|\n");
                    }
                }
                
            } finally {
                if (stdInput != null) {
                    stdInput.close();
                }
                if (intokens_reader != null) {
                    intokens_reader.close();
                }
                if (output != null) {
                    output.close();
                }
                if(p!=null){
                    p.getInputStream().close();
                    p.getOutputStream().close();
                    p.getErrorStream().close();
                    p.destroy();
                }
            }

        } catch (Exception e) {
            System.err.println("Errors found (TreeTagger):\n\t" + e.toString());
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }

        }


    }


    /**
     * Runs TreeTagger over a one-col PipesFile of Tokens (one token per line)
     * and saves the output in a .treetag file (PipesFile)
     *
     * Format word|pos|lemma
     *
     * NOTE: You can merge the information obtained in the desired column using MERGE_PIPES action
     *
     * @param filename
     * @return Output filename
     */
    public static String run_tok(String filename) {
        execute_tok(filename, filename + ".treetag");
        return filename + ".treetag";
    }
    
    public static String run_tok2(String filename) {
        execute_tok(filename, filename + ".tml");
        return filename + ".tml";
    }    
    private static void execute_tok(String filename, String outputfile) {
        try {
            //String fff=filename.replaceAll("\\\\","/")
            //                                                       .replaceAll("C:\\\\cygwin\\\\","");
            //System.out.println("aaa "+fff);
            //String[] command = {"C:\\cygwin\\bin\\sh", "ls"};
            //String[] command = {"ls"};
            //String[] command = {"ls","\"" + filename  +"\""};
            // TODO: ALTERNATIVE CREATE A SCRIPT THAT WRAPS all this?
            String[] command = {"C:\\cygwin\\bin\\sh.exe","-c","cat \"" + filename  +"\" | sed \"s/\\([^[:blank:]]\\)-\\([^[:blank:]]\\)/\\1 - \\2/g\" "};
                                                                   //| "+program_bin_tokenizer+" | sed \"s/[[:blank:]]\\+/\t/g\""};
            Process p = Runtime.getRuntime().exec(command);
            System.out.println("c:"+Arrays.toString(command));

            // TreeTagger Format
            // token  POS lemma (separated by \t)

            String treetag_line;
            BufferedWriter output = new BufferedWriter(new FileWriter(outputfile));
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try {
                while ((treetag_line = stdInput.readLine()) != null) {
                            System.out.println("line:"+treetag_line);
                            String[] treetag_arr = treetag_line.replaceAll("\\|", "-").split("\t");
                            if (treetag_arr.length == 3) {
                                    if(!treetag_arr[1].equalsIgnoreCase("SENT")){
                                        output.write(treetag_arr[0] + "|" + treetag_arr[1] + "|" + treetag_arr[2] + "\n");
                                    }else{
                                        output.write(treetag_arr[0] + "|" + treetag_arr[0] + "|" + treetag_arr[2] + "\n|\n");
                                    }
                            }else{
                                System.err.println("Error:"+treetag_line);
                            }
                }
                
            } finally {
                if (stdInput != null) {
                    stdInput.close();
                }
                if (output != null) {
                    output.close();
                }
                if(p!=null){
                    p.getOutputStream().close();
                    p.getErrorStream().close();
                    p.getInputStream().close();
                    p.destroy();
                }
            }

        } catch (Exception e) {
            System.err.println("Errors found (TreeTagger):\n\t" + e.toString());
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }

        }


    }














    /**
     * Runs TreeTagger over a multi-col PipesFile using tok_col rows as tokens
     * then merges the original file with the selected treetag_col puting it at the col position
     * and saves the output in a -treetag file (PipesFile)
     *
     * Treetag: word|pos|lemma (valid options are 1 for POS and 2 for lemma)
     *
     * NOTE: You can merge the information obtained in the desired column using MERGE_PIPES action
     *
     * @param filename
     * @param tok_col
     * @param treetag_col
     * @param col_position
     * @return Output filename
     */

    public static String run_and_merge(String filename, int tok_col, int treetag_col, int col_position) {
        try {
            String line;
            BufferedReader pipesreader = new BufferedReader(new FileReader(filename));
            File temptokfile = new File(filename + ".tok");
            BufferedWriter tokwriter = new BufferedWriter(new FileWriter(temptokfile));

            try {
                while ((line = pipesreader.readLine()) != null) {
                        String[] linearr = line.split("\\|");
                        if (linearr.length > tok_col) {
                            tokwriter.write(linearr[tok_col] + "\n");
                        }
                     else {
                        tokwriter.write("\n");
                    }

                }
            } finally {
                if (pipesreader != null) {
                    pipesreader.close();
                }
                if (tokwriter != null) {
                    tokwriter.close();
                }
            }

            execute_and_merge(filename,tok_col,treetag_col,col_position);
            temptokfile.delete();
        } catch (Exception e) {
            System.err.println("Errors found (TreeTagger):\n\t" + e.toString());
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
        return filename + "-treetag";

    }

        private static void execute_and_merge(String inputfile,int tok_col, int treetag_col, int col_position) {
        try {
            String[] command = {program_bin, "-token", "-lemma", program_model, inputfile+".tok"};
            Process p = Runtime.getRuntime().exec(command);

            // TreeTagger Format
            // token  POS lemma (separated by \t)
            String input_line;
            String treetag_line;
            BufferedReader intokens_reader = new BufferedReader(new FileReader(inputfile));
            BufferedWriter output = new BufferedWriter(new FileWriter(inputfile+"-treetag"));
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try {
                while ((input_line = intokens_reader.readLine()) != null) {
                    if (input_line.length() != 0 && !input_line.trim().equals("|")) {
                        if ((treetag_line = stdInput.readLine()) != null) {
                            String[] input_arr = input_line.split("\\|");
                            String[] treetag_arr = treetag_line.replaceAll("\\|", "-").split("\t");
                            if (treetag_arr.length == 3) {
                                if (treetag_arr[0].trim().equals(input_arr[tok_col].trim())) {
                                    for(int i=0;i<input_arr.length;i++){
                                        if(i>0){
                                            output.write('|');
                                        }
                                        if(i!=col_position){
                                            output.write(input_arr[i]);
                                        }else{
                                            output.write(treetag_arr[treetag_col]+"|"+input_arr[i]);
                                        }
                                    }
                                    if(input_arr.length<=col_position){
                                        output.write('|'+treetag_arr[treetag_col]);
                                    }
                                    output.write('\n');
                                } else {
                                    throw new Exception("Unexpected TreeTagger output token:" + treetag_arr[0].trim() + " - expected: " + input_arr[tok_col].trim());
                                }
                            } else {
                                throw new Exception("Malformed TreeTagger output:" + treetag_line);
                            }
                        } else {
                            throw new Exception("Unexpected end of TreeTagger output: tokens=" + input_line);
                        }
                    } else {
                        output.write("|\n");
                    }
                }
            
            } finally {
                if (stdInput != null) {
                    stdInput.close();
                }
                if (intokens_reader != null) {
                    intokens_reader.close();
                }
                if (output != null) {
                    output.close();
                }
                if(p!=null){
                    p.getInputStream().close();
                    p.getOutputStream().close();
                    p.getErrorStream().close();
                    p.destroy();
                }
            }

        } catch (Exception e) {
            System.err.println("Errors found (TreeTagger):\n\t" + e.toString());
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }

        }
        }

    public static String run_and_add(String filename, int tok_col) {
        try {
            String line;
            BufferedReader pipesreader = new BufferedReader(new FileReader(filename));
            File temptokfile = new File(filename + ".tok");
            BufferedWriter tokwriter = new BufferedWriter(new FileWriter(temptokfile));

            try {
                while ((line = pipesreader.readLine()) != null) {
                        String[] linearr = line.split("\\|");
                        if (linearr.length > tok_col) {
                            tokwriter.write(linearr[tok_col] + "\n");
                        }
                     else {
                        tokwriter.write("\n");
                    }

                }
            } finally {
                if (pipesreader != null) {
                    pipesreader.close();
                }
                if (tokwriter != null) {
                    tokwriter.close();
                }
            }

            execute_and_add(filename,tok_col);
            temptokfile.delete();
        } catch (Exception e) {
            System.err.println("Errors found (TreeTagger):\n\t" + e.toString());
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
        return filename + "-treetag";

    }

        private static void execute_and_add(String inputfile,int tok_col) {
        try {
            String[] command = {program_bin, "-token", "-lemma", program_model, inputfile+".tok"};
            Process p = Runtime.getRuntime().exec(command);

            // TreeTagger Format
            // token  POS lemma (separated by \t)
            String input_line;
            String treetag_line;
            BufferedReader intokens_reader = new BufferedReader(new FileReader(inputfile));
            BufferedWriter output = new BufferedWriter(new FileWriter(inputfile+"-treetag"));
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try {
                while ((input_line = intokens_reader.readLine()) != null) {
                    if (input_line.length() != 0 && !input_line.trim().equals("|")) {
                        if ((treetag_line = stdInput.readLine()) != null) {
                            String[] input_arr = input_line.split("\\|");
                            String[] treetag_arr = treetag_line.replaceAll("\\|", "-").split("\t");
                            if (treetag_arr.length == 3) {
                                if (treetag_arr[0].trim().equals(input_arr[tok_col].trim())) {
                                    for(int i=0;i<input_arr.length;i++){
                                        if(i>0){
                                            output.write('|');
                                        }
                                        output.write(input_arr[i]);
                                    }
                                    if(!treetag_arr[1].equalsIgnoreCase("SENT")){
                                        output.write('|'+treetag_arr[1] + '|' + treetag_arr[2]+"\n");
                                    }else{
                                        output.write('|'+treetag_arr[0] + '|' + treetag_arr[2]+"\n");
                                    }
                                } else {
                                    throw new Exception("Unexpected TreeTagger output token:" + treetag_arr[0].trim() + " - expected: " + input_arr[tok_col].trim());
                                }
                            } else {
                                throw new Exception("Malformed TreeTagger output:" + treetag_line);
                            }
                        } else {
                            throw new Exception("Unexpected end of TreeTagger output: tokens=" + input_line);
                        }
                    } else {
                        output.write("|\n");
                    }
                }
            
            } finally {
                if (stdInput != null) {
                    stdInput.close();
                }
                if (intokens_reader != null) {
                    intokens_reader.close();
                }
                if (output != null) {
                    output.close();
                }
                if(p!=null){
                    p.getInputStream().close();
                    p.getOutputStream().close();
                    p.getErrorStream().close();
                    p.destroy();
                }
            }

        } catch (Exception e) {
            System.err.println("Errors found (TreeTagger):\n\t" + e.toString());
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }

        }
        }



}
