package com.cognitionis.tipsem;

import com.cognitionis.external_tools.*;
import com.cognitionis.feature_builder.*;
import com.cognitionis.utils_basickit.FileUtils;
import com.cognitionis.nlp_files.NLPFile;
import com.cognitionis.nlp_files.TempEvalFiles;
import com.cognitionis.nlp_files.PipesFile;
import com.cognitionis.timeml_basickit.Link;
import com.cognitionis.timeml_basickit.Timex;
import com.cognitionis.timeml_basickit.Event;
import java.io.*;
import java.text.*;
import java.util.*;
import org.joda.time.DateTime;

/**
 * @author Hector Llorens
 * @since 2011
 */
public class TIP {

    public static String get_last_text_blanks(String file) {
        String last_text_blanks = "\n";
        try {
            File f = new File(file);
            RandomAccessFile raf = new RandomAccessFile(f, "r");
            //String filecontents = null;
            //filecontents = FileUtils.readFileAsString(file, "UTF-8");
            /*if(filecontents.endsWith("\n")){
            filecontents=filecontents.substring(0, filecontents.length()-1);
            }*/
            /*last_text_blanks = filecontents.replaceAll(".+(\\s+)$", ".$1.");
            if(filecontents.equals(last_text_blanks)){
            last_text_blanks="\n";
            }*/
            for (long i = f.length() - 2; i > 0; i--) {
                raf.seek(i);
                char c = (char) raf.readByte();
                if (c == '\n' || c == '\r' || c == '\t' || c == ' ') {
                    last_text_blanks = c + last_text_blanks;
                } else {
                    break;
                }
            }
            if(raf!=null){
                raf.close();
            }
        } catch (Exception e) {
            System.err.println("Errors found (TML_file_utils):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
            }
            return null;
        }
        return last_text_blanks;
    }


    /**
     * Annotate a plain or te3input file, specifying: the file, intput format, approach, language, entities to annotate,
     * dctvalue, and the path to the models used for annotating.
     *
     * @param nlpfile
     * @param inputf
     * @param approach
     * @param lang
     * @param entities
     * @param dctvalue
     * @param models_path
     * @return
     */
    public static String annotate(NLPFile nlpfile, String inputf, String approach, String lang, String entities, String dctvalue, String models_path) {
        String ret = null;
        try {
            if (entities == null) {
                entities = "timex;event;tlink";
            }
            if (models_path == null || models_path.equals("")) {
                models_path = "";
            } else {
                models_path += File.separator;
            }

            String header = null;
            String footer = null;
            String last_text_blanks = "";

            Timex dct = null;
            if (inputf.equals("plain")) {
                if (!nlpfile.getClass().getSimpleName().equals("PlainFile")) {
                    throw new Exception("TIPSem requires PlainFile files as input. Found: " + nlpfile.getClass().getSimpleName());
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                if (dctvalue == null) {
                    dctvalue = sdf.format(new Date());
                    dct = new Timex("t0", dctvalue, "DATE", dctvalue, nlpfile.getFile().getName(), -1, -1, true);
                } else {
                    sdf.setLenient(false);
                    Date tmpdct = null;
                    if ((tmpdct = sdf.parse(dctvalue)) != null) {
                        dctvalue = sdf.format(tmpdct);
                        dct = new Timex("t0", dctvalue, "DATE", dctvalue, nlpfile.getFile().getName(), -1, -1, true);
                    } else {
                        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                        if ((tmpdct = sdf2.parse(dctvalue)) != null) {
                            dctvalue = sdf2.format(tmpdct);
                            dct = new Timex("t0", dctvalue, "DATE", dctvalue, nlpfile.getFile().getName(), -1, -1, true);
                        }
                    }
                }
                // read last banks

            } else {
                if (!nlpfile.getExtension().equalsIgnoreCase("tml")) {
                    nlpfile.overrideExtension("tml");
                }
                if (!nlpfile.isWellFormatted()) {
                    throw new Exception("File: " + nlpfile.getFile() + " is not a valid TimeML (.tml) XML file.");
                }
                String line;
                boolean textfound = false;
                header = "";
                footer = "";

                //process header (and dct)/text/footer
                BufferedWriter textwriter = new BufferedWriter(new FileWriter(new File(nlpfile.getFile().getCanonicalPath() + ".txt")));
                BufferedReader TE3inputReader = new BufferedReader(new FileReader(nlpfile.getFile()));

                try {

                    // read out header
                    while ((line = TE3inputReader.readLine()) != null) {
                        if (line.length() > 0) {
                            // break on TEXT
                            if (line.matches(".*<TEXT>.*")) {
                                textfound = true;
                                break;
                            }
                            // check DCT
                            if (line.matches(".*<DCT>.*")) {
                                String tid = line.substring(line.indexOf("tid=\"") + 5, line.indexOf("\"", line.indexOf("tid=\"") + 5));
                                String type = line.substring(line.indexOf("type=\"") + 6, line.indexOf("\"", line.indexOf("type=\"") + 6));
                                String value = line.substring(line.indexOf("value=\"") + 7, line.indexOf("\"", line.indexOf("value=\"") + 7));
                                dct = new Timex(tid, value, type, value, nlpfile.getFile().getName(), -1, -1, true);
                            }
                            // DOCID is not needed since filename can be used instead
                            // (we are not treating multi-files in one file)
                            // (wich is multiple <TimeML> tags
                        }
                        header += line + "\n";
                    }

                    if (!textfound) {
                        throw new Exception("Premature end of file (" + nlpfile.getFile().getName() + ")");
                    }

                    // read out text
                    while ((line = TE3inputReader.readLine()) != null) {
                        if (line.length() > 0) {
                            // break on TEXT
                            if (line.matches(".*</TEXT>.*")) {
                                textfound = false;
                                break;
                            }
                        }
                        // unescape
                        textwriter.write(line.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">") + "\n");
                    }

                    if (textfound) {
                        throw new Exception("Premature end of file (" + nlpfile.getFile().getName() + ")");
                    }

                    // read out footer
                    while ((line = TE3inputReader.readLine()) != null) {
                        footer += line + "\n";
                    }

                    // DCT (will be improved in the future to distinguish non-DCT docs)
                    if (dct == null) {
                        dct = new Timex("t0", "dct", "DATE", new DateTime().toString("YYYY-MM-dd"), nlpfile.getFile().getName(), -1, -1, true);
                    }

                } finally {
                    if (TE3inputReader != null) {
                        TE3inputReader.close();
                    }
                    if (textwriter != null) {
                        textwriter.close();
                    }
                }
            }
            File dir = new File(nlpfile.getFile().getCanonicalPath() + "_" + approach + "_features" + File.separator);
            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdir();
            }
            String output = dir + "/" + nlpfile.getFile().getName();
            if (inputf.equals("plain")) {
                FileUtils.copyFileUtil(nlpfile.getFile(), new File(output));
            } else {
                (new File(nlpfile.getFile().getCanonicalPath() + ".txt")).renameTo(new File(output));
            }

            last_text_blanks = get_last_text_blanks(output);


            // Write dct.tab??? THIS CAN BE REMOVED IF getTIMEN includes the dct as parameter...
            BufferedWriter dct_writer = new BufferedWriter(new FileWriter(new File(dir + "/" + "dct.tab")));
            try {
                dct_writer.write(nlpfile.getFile().getName() + "\t" + dct.get_value());
            } finally {
                if (dct_writer != null) {
                    dct_writer.close();
                }
            }

            String features = null;
            features = BaseTokenFeatures.getFeatures4Plain(lang, output, 1, false, "TempEval2-features", approach);

            String timex_merged = null;
            if (entities.contains("timex")) {
                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.print("Recognizing TIMEX3s");
                }
                String timex = CRF.test(features, models_path + approach + "_rec_timex_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                PipesFile nlpfile_temp = new PipesFile(timex);
                ((PipesFile) nlpfile_temp).isWellFormedOptimist();
                timex = PipesFile.IOB2check(nlpfile_temp);

                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.print("Classifying TIMEX3s");
                }
                output = Classification.get_classik(timex, lang);
                String timex_class = SVM.test(output, models_path + approach + "_class_timex_" + nlpfile.getLanguage().toUpperCase() + ".SVMmodel");

                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.print("Normalizing TIMEX3s (DCT=" + dct.get_value() + ")");
                }
                output = TimexNormalization.getTIMEN(timex, timex_class, lang);
                output = SVM.test(output, models_path + approach + "_timen_timex_" + nlpfile.getLanguage().toUpperCase() + ".SVMmodel");
                String timex_norm = TimexNormalization.get_normalized_values(output, lang);

                output = TempEvalFiles.merge_classik(timex, timex_class, "type");
                timex_merged = BaseTokenFeatures.merge_classik_append(output, timex_norm, "value");
            }

            String event_merged = null;
            if (entities.contains("event")) {
                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.print("Recognizing EVENTs");
                }
                String event = CRF.test(features, models_path + approach + "_rec_event_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                PipesFile nlpfile_temp = new PipesFile(event);
                ((PipesFile) nlpfile_temp).isWellFormedOptimist();
                event = PipesFile.IOB2check(nlpfile_temp);

                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.print("Classifying EVENTs");
                }
                output = Classification.get_classik(event, lang);
                String event_class = SVM.test(output, models_path + approach + "_class_event_" + nlpfile.getLanguage().toUpperCase() + ".SVMmodel");

                event_merged = TempEvalFiles.merge_classik(event, event_class, "class");
            }
            // Omit signals for the moment: wait for longer and better corpus
                        /*String signal;
            if(lang.equalsIgnoreCase("EN")){
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
            System.err.print("Recognizing SIGNALS");
            }
            signal = CRF.test(features, model+"_rec_signal_"+nlpfile.getLanguage().toUpperCase()+".CRFmodel");
            nlpfile_temp = new PipesFile();
            nlpfile_temp.loadFile(new File(signal));
            ((PipesFile) nlpfile_temp).isWellFormedOptimist();
            signal=PipesFile.IOB2check(nlpfile_temp);
            all_merged=PipesFile.merge_pipes(all_merged,signal);
            }*/

            String all_merged = PipesFile.merge_pipes(timex_merged, event_merged);
            all_merged = BaseTokenFeatures.putids(all_merged);

            PipesFile pf = new PipesFile(all_merged);
            pf.isWellFormedOptimist();


            HashMap<String, Timex> DCTs = new HashMap<String, Timex>();
            if (dct != null) {
                DCTs.put(nlpfile.getFile().getName(), dct);
            }
            HashMap<String, HashMap<String, Timex>> timexes = new HashMap<String, HashMap<String, Timex>>();
            HashMap<String, HashMap<String, Event>> events = new HashMap<String, HashMap<String, Event>>();
            HashMap<String, HashMap<String, Event>> makeinstances = new HashMap<String, HashMap<String, Event>>();
            HashMap<String, HashMap<String, Link>> links = new HashMap<String, HashMap<String, Link>>(); // et, e-dct (main and reporting), main (prev sent or last main), sub (intra sent)


            if (entities.contains("tlink")) {
                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.println("Recognizing TLINKs");
                }
                // In this case all is only one file and the DCT is only one timex and the doc_id is the file
                // TODO: In the future manage DCT news vs history
                // generate possible links files
                ElementFiller.get_elements(pf, DCTs, timexes, events, makeinstances, links, null);
                String basefile = all_merged.substring(0, all_merged.indexOf(".TempEval2-features"));
                // categorize them
                if (!entities.contains("tlinkspecial")) {
                    String etlinks = SVM.test(basefile + ".e-t-link-features", models_path + approach + "_categ_e-t_" + nlpfile.getLanguage().toUpperCase() + ".SVMmodel");
                    String edctlinks = SVM.test(basefile + ".e-dct-link-features", models_path + approach + "_categ_e-dct_" + nlpfile.getLanguage().toUpperCase() + ".SVMmodel");
                    String emainlinks = CRF.test(basefile + ".e-main-link-features", models_path + approach + "_categ_e-main_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                    String esublinks = CRF.test(basefile + ".e-sub-link-features", models_path + approach + "_categ_e-sub_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                    ElementFiller.updateLinks(etlinks, edctlinks, emainlinks, esublinks, links);
                } else {
                    System.out.println("Unavailable for now");
                }

            }
            // TML output: add links to the output
            output = FileConverter.pipes2tml(pf, DCTs, makeinstances, links, header, footer, last_text_blanks);
            ret = output;

        } catch (Exception e) {
            System.err.println("\nErrors found (TIP):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
            return null;
        }
        return ret;
    }


    /**
     * Annotate a plain or te3input file, specifying: the file, intput format, approach, language, entities to annotate,
     * dctvalue, and the path to the models used for annotating.
     *
     * @param nlpfile
     * @param inputf
     * @param approach
     * @param lang
     * @param entities
     * @param dctvalue
     * @param models_path
     * @return
     */
    public static String annotateCRF(NLPFile nlpfile, String inputf, String approach, String lang, String entities, String dctvalue, String models_path) {
        String ret = null;
        try {
            if (entities == null) {
                entities = "timex;event;tlink";
            }
            if (models_path == null || models_path.equals("")) {
                models_path = "";
            } else {
                models_path += File.separator;
            }

            String header = null;
            String footer = null;
            String last_text_blanks = "";

            Timex dct = null;
            if (inputf.equals("plain")) {
                if (!nlpfile.getClass().getSimpleName().equals("PlainFile")) {
                    throw new Exception("TIPSem requires PlainFile files as input. Found: " + nlpfile.getClass().getSimpleName());
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                if (dctvalue == null) {
                    dctvalue = sdf.format(new Date());
                    dct = new Timex("t0", dctvalue, "DATE", dctvalue, nlpfile.getFile().getName(), -1, -1, true);
                } else {
                    sdf.setLenient(false);
                    Date tmpdct = null;
                    if ((tmpdct = sdf.parse(dctvalue)) != null) {
                        dctvalue = sdf.format(tmpdct);
                        dct = new Timex("t0", dctvalue, "DATE", dctvalue, nlpfile.getFile().getName(), -1, -1, true);
                    } else {
                        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                        if ((tmpdct = sdf2.parse(dctvalue)) != null) {
                            dctvalue = sdf2.format(tmpdct);
                            dct = new Timex("t0", dctvalue, "DATE", dctvalue, nlpfile.getFile().getName(), -1, -1, true);
                        }
                    }
                }
                // read last banks

            } else {
                if (!nlpfile.getExtension().equalsIgnoreCase("tml")) {
                    nlpfile.overrideExtension("tml");
                }
                if (!nlpfile.isWellFormatted()) {
                    throw new Exception("File: " + nlpfile.getFile() + " is not a valid TimeML (.tml) XML file.");
                }
                String line;
                boolean textfound = false;
                header = "";
                footer = "";

                //process header (and dct)/text/footer
                BufferedWriter textwriter = new BufferedWriter(new FileWriter(new File(nlpfile.getFile().getCanonicalPath() + ".txt")));
                BufferedReader TE3inputReader = new BufferedReader(new FileReader(nlpfile.getFile()));

                try {

                    // read out header
                    while ((line = TE3inputReader.readLine()) != null) {
                        if (line.length() > 0) {
                            // break on TEXT
                            if (line.matches(".*<TEXT>.*")) {
                                textfound = true;
                                break;
                            }
                            // check DCT
                            if (line.matches(".*<DCT>.*")) {
                                String tid = line.substring(line.indexOf("tid=\"") + 5, line.indexOf("\"", line.indexOf("tid=\"") + 5));
                                String type = line.substring(line.indexOf("type=\"") + 6, line.indexOf("\"", line.indexOf("type=\"") + 6));
                                String value = line.substring(line.indexOf("value=\"") + 7, line.indexOf("\"", line.indexOf("value=\"") + 7));
                                dct = new Timex(tid, value, type, value, nlpfile.getFile().getName(), -1, -1, true);
                            }
                            // DOCID is not needed since filename can be used instead
                            // (we are not treating multi-files in one file)
                            // (wich is multiple <TimeML> tags
                        }
                        header += line + "\n";
                    }

                    if (!textfound) {
                        throw new Exception("Premature end of file (" + nlpfile.getFile().getName() + ")");
                    }

                    // read out text
                    while ((line = TE3inputReader.readLine()) != null) {
                        if (line.length() > 0) {
                            // break on TEXT
                            if (line.matches(".*</TEXT>.*")) {
                                textfound = false;
                                break;
                            }
                        }
                        // unescape
                        textwriter.write(line.replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">") + "\n");
                    }

                    if (textfound) {
                        throw new Exception("Premature end of file (" + nlpfile.getFile().getName() + ")");
                    }

                    // read out footer
                    while ((line = TE3inputReader.readLine()) != null) {
                        footer += line + "\n";
                    }

                    // DCT (will be improved in the future to distinguish non-DCT docs)
                    if (dct == null) {
                        dct = new Timex("t0", "dct", "DATE", new DateTime().toString("YYYY-MM-dd"), nlpfile.getFile().getName(), -1, -1, true);
                    }

                } finally {
                    if (TE3inputReader != null) {
                        TE3inputReader.close();
                    }
                    if (textwriter != null) {
                        textwriter.close();
                    }
                }
            }
            File dir = new File(nlpfile.getFile().getCanonicalPath() + "_" + approach + "_features" + File.separator);
            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdir();
            }
            String output = dir + "/" + nlpfile.getFile().getName();
            if (inputf.equals("plain")) {
                FileUtils.copyFileUtil(nlpfile.getFile(), new File(output));
            } else {
                (new File(nlpfile.getFile().getCanonicalPath() + ".txt")).renameTo(new File(output));
            }

            last_text_blanks = get_last_text_blanks(output);


            // Write dct.tab??? THIS CAN BE REMOVED IF getTIMEN includes the dct as parameter...
            BufferedWriter dct_writer = new BufferedWriter(new FileWriter(new File(dir + "/" + "dct.tab")));
            try {
                dct_writer.write(nlpfile.getFile().getName() + "\t" + dct.get_value());
            } finally {
                if (dct_writer != null) {
                    dct_writer.close();
                }
            }

            String features = null;
            features = BaseTokenFeatures.getFeatures4Plain(lang, output, 1, false, "TempEval2-features", approach);

            String timex_merged = null;
            if (entities.contains("timex")) {
                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.print("Recognizing TIMEX3s");
                }
                String timex = CRF.test(features, models_path + approach + "_rec_timex_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                PipesFile nlpfile_temp = new PipesFile(timex);
                ((PipesFile) nlpfile_temp).isWellFormedOptimist();
                timex = PipesFile.IOB2check(nlpfile_temp);

                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.print("Classifying TIMEX3s");
                }
                output = Classification.get_classik(timex, lang);
                String timex_class = CRF.test(output, models_path + approach + "_class_timex_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");

                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.print("Normalizing TIMEX3s (DCT=" + dct.get_value() + ")");
                }
                output = TimexNormalization.getTIMEN(timex, timex_class, lang);
                output = CRF.test(output, models_path + approach + "_timen_timex_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                String timex_norm = TimexNormalization.get_normalized_values(output, lang);

                output = TempEvalFiles.merge_classik(timex, timex_class, "type");
                timex_merged = BaseTokenFeatures.merge_classik_append(output, timex_norm, "value");
            }

            String event_merged = null;
            if (entities.contains("event")) {
                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.print("Recognizing EVENTs");
                }
                String event = CRF.test(features, models_path + approach + "_rec_event_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                PipesFile nlpfile_temp = new PipesFile(event);
                ((PipesFile) nlpfile_temp).isWellFormedOptimist();
                event = PipesFile.IOB2check(nlpfile_temp);

                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.print("Classifying EVENTs");
                }
                output = Classification.get_classik(event, lang);
                String event_class = CRF.test(output, models_path + approach + "_class_event_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");

                event_merged = TempEvalFiles.merge_classik(event, event_class, "class");
            }
            // Omit signals for the moment: wait for longer and better corpus
                        /*String signal;
            if(lang.equalsIgnoreCase("EN")){
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
            System.err.print("Recognizing SIGNALS");
            }
            signal = CRF.test(features, model+"_rec_signal_"+nlpfile.getLanguage().toUpperCase()+".CRFmodel");
            nlpfile_temp = new PipesFile();
            nlpfile_temp.loadFile(new File(signal));
            ((PipesFile) nlpfile_temp).isWellFormedOptimist();
            signal=PipesFile.IOB2check(nlpfile_temp);
            all_merged=PipesFile.merge_pipes(all_merged,signal);
            }*/

            String all_merged = PipesFile.merge_pipes(timex_merged, event_merged);
            all_merged = BaseTokenFeatures.putids(all_merged);

            PipesFile pf = new PipesFile(all_merged);
            pf.isWellFormedOptimist();


            HashMap<String, Timex> DCTs = new HashMap<String, Timex>();
            if (dct != null) {
                DCTs.put(nlpfile.getFile().getName(), dct);
            }
            HashMap<String, HashMap<String, Timex>> timexes = new HashMap<String, HashMap<String, Timex>>();
            HashMap<String, HashMap<String, Event>> events = new HashMap<String, HashMap<String, Event>>();
            HashMap<String, HashMap<String, Event>> makeinstances = new HashMap<String, HashMap<String, Event>>();
            HashMap<String, HashMap<String, Link>> links = new HashMap<String, HashMap<String, Link>>(); // et, e-dct (main and reporting), main (prev sent or last main), sub (intra sent)


            if (entities.contains("tlink")) {
                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.println("Recognizing TLINKs");
                }
                // In this case all is only one file and the DCT is only one timex and the doc_id is the file
                // TODO: In the future manage DCT news vs history
                // generate possible links files
                ElementFiller.get_elements(pf, DCTs, timexes, events, makeinstances, links, null);
                String basefile = all_merged.substring(0, all_merged.indexOf(".TempEval2-features"));
                // categorize them
                if (!entities.contains("tlinkspecial")) {
                    String etlinks = CRF.test(basefile + ".e-t-link-features", models_path + approach + "_categ_e-t_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                    String edctlinks = CRF.test(basefile + ".e-dct-link-features", models_path + approach + "_categ_e-dct_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                    String emainlinks = CRF.test(basefile + ".e-main-link-features", models_path + approach + "_categ_e-main_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                    String esublinks = CRF.test(basefile + ".e-sub-link-features", models_path + approach + "_categ_e-sub_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                    ElementFiller.updateLinks(etlinks, edctlinks, emainlinks, esublinks, links);
                } else {
                    System.out.println("Unavailable for now");
                }

            }
            // TML output: add links to the output
            output = FileConverter.pipes2tml(pf, DCTs, makeinstances, links, header, footer, last_text_blanks);
            ret = output;

        } catch (Exception e) {
            System.err.println("\nErrors found (TIP):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
            return null;
        }
        return ret;
    }

    /**
     * Annotate a plain or te3input file, specifying: the file, intput format, approach, language, entities to annotate,
     * dctvalue, and the path to the models used for annotating.
     *
     * @param nlpfile
     * @param inputf
     * @param approach
     * @param lang
     * @param entities
     * @param dctvalue
     * @param models_path
     * @return
     */
    public static String annotate_links(NLPFile nlpfile, String approach, String lang, String models_path, String strategy) {
        String ret = null;
        try {
            if (models_path == null || models_path.equals("")) {
                models_path = "";
            } else {
                models_path += File.separator;
            }

            if(strategy==null) strategy="normal";

            String header = null;
            String footer = null;
            String last_text_blanks = "";

            Timex dct = null;

            if (!nlpfile.getExtension().equalsIgnoreCase("tml")) {
                nlpfile.overrideExtension("tml");
            }
            if (!nlpfile.isWellFormatted()) {
                throw new Exception("File: " + nlpfile.getFile() + " is not a valid TimeML (.tml) XML file.");
            }
            String line;
            boolean textfound = false;
            header = "";
            footer = "";

            //process header (and dct)/text/footer
            BufferedWriter textwriter = new BufferedWriter(new FileWriter(new File(nlpfile.getFile().getCanonicalPath() + ".txt")));
            BufferedReader TE3inputReader = new BufferedReader(new FileReader(nlpfile.getFile()));

            try {

                // read out header
                while ((line = TE3inputReader.readLine()) != null) {
                    if (line.length() > 0) {
                        // break on TEXT
                        if (line.matches(".*<TEXT>.*")) {
                            textfound = true;
                            break;
                        }
                        // check DCT
                        if (line.matches(".*<DCT>.*")) {
                            String tid = line.substring(line.indexOf("tid=\"") + 5, line.indexOf("\"", line.indexOf("tid=\"") + 5));
                            String type = line.substring(line.indexOf("type=\"") + 6, line.indexOf("\"", line.indexOf("type=\"") + 6));
                            String value = line.substring(line.indexOf("value=\"") + 7, line.indexOf("\"", line.indexOf("value=\"") + 7));
                            dct = new Timex(tid, value, type, value, nlpfile.getFile().getName(), -1, -1, true);
                        }
                        // DOCID is not needed since filename can be used instead
                        // (we are not treating multi-files in one file)
                        // (wich is multiple <TimeML> tags
                    }
                    header += line + "\n";
                }

                if (!textfound) {
                    throw new Exception("Premature end of file (" + nlpfile.getFile().getName() + ")");
                }

                // read out text
                while ((line = TE3inputReader.readLine()) != null) {
                    if (line.length() > 0) {
                        // break on TEXT
                        if (line.matches(".*</TEXT>.*")) {
                            textfound = false;
                            break;
                        }
                    }
                    // untag and unescape
                    textwriter.write(line.replaceAll("<[^>]*>", "").replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">") + "\n");
                }

                if (textfound) {
                    throw new Exception("Premature end of file (" + nlpfile.getFile().getName() + ")");
                }

                // read out footer
                while ((line = TE3inputReader.readLine()) != null) {
                    footer += line + "\n";
                }

                // DCT (will be improved in the future to distinguish non-DCT docs)
                if (dct == null) {
                    dct = new Timex("t0", "dct", "DATE", new DateTime().toString("YYYY-MM-dd"), nlpfile.getFile().getName(), -1, -1, true);
                }

            } finally {
                if (TE3inputReader != null) {
                    TE3inputReader.close();
                }
                if (textwriter != null) {
                    textwriter.close();
                }
            }

            File dir = new File(nlpfile.getFile().getCanonicalPath() + "_" + approach + "_features" + File.separator);
            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdir();
            }
            String output = dir + "/" + nlpfile.getFile().getName();

            (new File(nlpfile.getFile().getCanonicalPath() + ".txt")).renameTo(new File(output));


            last_text_blanks = get_last_text_blanks(output);


            // Write dct.tab??? THIS CAN BE REMOVED IF getTIMEN includes the dct as parameter...
            BufferedWriter dct_writer = new BufferedWriter(new FileWriter(new File(dir + "/" + "dct.tab")));
            try {
                dct_writer.write(nlpfile.getFile().getName() + "\t" + dct.get_value());
            } finally {
                if (dct_writer != null) {
                    dct_writer.close();
                }
            }

            String features = null;
            features = BaseTokenFeatures.getFeatures4Plain(lang, output, 1, false, "TempEval2-features", approach);
            NLPFile featuresfile=new PipesFile(features);
            featuresfile.isWellFormatted();
            featuresfile.setLanguage(lang);
            String all_merged = ((PipesFile) featuresfile).merge_tok_n_xml(nlpfile.getFile().getCanonicalPath(), "TEXT", ".*", ".*", null);
            PipesFile pf = new PipesFile(all_merged);
            pf.isWellFormedOptimist();

            HashMap<String, Timex> DCTs = new HashMap<String, Timex>();
            if (dct != null) {
                DCTs.put(nlpfile.getFile().getName(), dct);
            }
            HashMap<String, HashMap<String, Timex>> timexes = new HashMap<String, HashMap<String, Timex>>();
            HashMap<String, HashMap<String, Event>> events = new HashMap<String, HashMap<String, Event>>();
            HashMap<String, HashMap<String, Event>> makeinstances = new HashMap<String, HashMap<String, Event>>();
            HashMap<String, HashMap<String, Link>> links = new HashMap<String, HashMap<String, Link>>(); // et, e-dct (main and reporting), main (prev sent or last main), sub (intra sent)


            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                System.err.println("Recognizing TLINKs");
            }
            // In this case all is only one file and the DCT is only one timex and the doc_id is the file
            // TODO: In the future manage DCT news vs history
            // generate possible links files
            ElementFiller.get_elements(pf, DCTs, timexes, events, makeinstances, links, null);
            String basefile = all_merged.substring(0, all_merged.indexOf(".TempEval2-features"));
            // categorize them
            if (strategy.equals("super-baseline")) {
                String etlinks = categorize_baseline(basefile + ".e-t-link-features", "IS_INCLUDED");
                String edctlinks = categorize_baseline(basefile + ".e-dct-link-features", "BEFORE");
                String emainlinks = categorize_baseline(basefile + ".e-main-link-features", "SIMULTANEOUS");
                String esublinks = categorize_baseline(basefile + ".e-sub-link-features", "BEFORE");
                ElementFiller.updateLinks(etlinks, edctlinks, emainlinks, esublinks, links);
            }else{
                String etlinks = SVM.test(basefile + ".e-t-link-features", models_path + approach + "_categ_e-t_" + nlpfile.getLanguage().toUpperCase() + ".SVMmodel");
                String edctlinks = SVM.test(basefile + ".e-dct-link-features", models_path + approach + "_categ_e-dct_" + nlpfile.getLanguage().toUpperCase() + ".SVMmodel");
                String emainlinks = CRF.test(basefile + ".e-main-link-features", models_path + approach + "_categ_e-main_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                String esublinks = CRF.test(basefile + ".e-sub-link-features", models_path + approach + "_categ_e-sub_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                ElementFiller.updateLinks(etlinks, edctlinks, emainlinks, esublinks, links);
            }
            
            // TML output: add links to the output
            output = FileConverter.pipes2tml(pf, DCTs, makeinstances, links, header, footer, last_text_blanks);
            ret = output;

        } catch (Exception e) {
            System.err.println("\nErrors found (TIP):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
            return null;
        }
        return ret;
    }

    public static String categorize_baseline(String features, String mfcategory){
        String output=features+"-annotatedWith-Baseline";
        try{
                BufferedReader reader = new BufferedReader(new FileReader(features));
                BufferedWriter textwriter = new BufferedWriter(new FileWriter(new File(output)));
                try {
                    String line="";
                    while ((line = reader.readLine()) != null) {
                        if (line.length() > 0) {
                            textwriter.write(line+"|"+mfcategory+"\n");
                        }
                    }
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                    if (textwriter != null) {
                        textwriter.close();
                    }
                }
        } catch (Exception e) {
            System.err.println("\nErrors found (TIP):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
            return null;
        }
        return output;
    }

}
