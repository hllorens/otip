package com.cognitionis.tipsem;

import com.cognitionis.external_tools.*;
import com.cognitionis.feature_builder.*;
import com.cognitionis.nlp_files.NLPFile;
import com.cognitionis.nlp_files.XMLFile;
import com.cognitionis.nlp_files.TempEvalFiles;
import com.cognitionis.nlp_files.TabFile;
import com.cognitionis.nlp_files.PipesFile;
import com.cognitionis.nlp_files.PlainFile;
import com.cognitionis.timeml_basickit.TML_file_utils;
import com.cognitionis.timeml_basickit.Link;
import com.cognitionis.timeml_basickit.Timex;
import com.cognitionis.timeml_basickit.Event;
import com.cognitionis.utils_basickit.FileUtils;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import com.cognitionis.utils_basickit.StringUtils;
import com.cognitionis.wiki_basickit.Wiki_bk;

/**
 * @author Hector Llorens
 * @since 2011
 */
public class OptionHandler {

    public static String localDatasetPath = FileUtils.getApplicationPath() + "program-data" + File.separator + "TIMEE-training" + File.separator;

    public static enum Action {
        ANNOTATECRF,
        ANNOTATE, TAN, //TIMEX, EVENT, TE3,   GET_FEATURES4PLAIN, GET_FEATURES4TAB,   TIMEKPRUEBAS,   SEGMENT_SENTENCE_PRUEBAS
        TRAIN_AND_TEST, //TRAIN_RECOGNITION_TML, TRAIN_CLASSIFICATION_TML, TRAIN_NORMALIZATIONTYPE_TML, TRAIN_CATEGORIZATION_TML,
        TEST_NORMALIZATION_TML, //TRAIN_RECOGNITION, TRAIN_CLASSIFICATION, TRAIN_NORMALIZATIONTYPE, TRAIN_CATEGORIZATION, TEST_NORMALIZATION,
        //TML2ISOTML, ISOTML2TML, TML2TE3, // can be unified in one
        PLAIN2TE3,
        CONVERT_TML, DATASET2TML, RECOGNITION2TML,
        //DATASET2REMOVE_ES_TOBE_STATES, DATASET2TML, TML2DATASET4MODEL, TMLDIR2DATASET4MODEL,   // se puede eliminar o unir con las anteriores
        UTF_2_ISO_8859_1_SAFE_UTF,
        TML_NFOLD //, TAB_NFOLD, TEMPEVAL_RECOGNITION_10FOLD, TEMPEVAL_CLASSIFICATION_10FOLD,
        ;
    }

    /* IF A COMPANY WANTS TIPSEM:
     * TRY TO FIND STANDARD SENTENCE SEGMENTER JAVA (LIKE TREETAGER ALGORITHM WITH CERTAIN ABREVIATION MANAGEMENT...
     * TRY TO FIND JAVA STANDARD TOKENIZER (LIKE TREETAGER ALGORITHM WITH GOOD AND PREDICTABLE-STABLE BEHAVIOUR)
     * TRY TO FIND JAVA NATIVE CRF, SVM, ETC.., AND POS, AND SYTN-DEP, LEMMA...
     * Replace SRL AND Treetagger otherwise IT HAS TO ACCEPT AND PROBABLY PAY Treetagger and SRL
     */
    public static String getParameter(String params, String param) {
        String paramValue = null;
        if (params != null && params.contains(param)) {
            if (params.matches(".*" + param + "=[^,]*,.*")) {
                paramValue = params.substring(params.lastIndexOf(param + "=") + param.length() + 1, params.indexOf(',', params.lastIndexOf(param + "=")));
            } else {
                if (params.matches(".*" + param + "=[^,]*")) {
                    paramValue = params.substring(params.lastIndexOf(param + "=") + param.length() + 1);
                }
            }
        }
        return paramValue;
    }

    public static void doAction(String action, String[] input_files, String action_parameters, String lang) {

        try {
            System.err.println("\n\nDoing action: " + action.toUpperCase() + "\n------------");
            switch (Action.valueOf(action.toUpperCase())) {

                case UTF_2_ISO_8859_1_SAFE_UTF:
                    for (int i = 0; i < input_files.length; i++) {
                        String line;
                        BufferedReader TE3inputReader = new BufferedReader(new FileReader(new File(input_files[i])));
                        try {
                            System.err.println("Processing file: " + input_files[i]);
                            while ((line = TE3inputReader.readLine()) != null) {
                                if (!StringUtils.existsInEncoding(line, "ISO-8859-1")) {
                                    throw new Exception("Not ISO safe UTF line: " + line);
                                }
                            }
                        } finally {
                            if (TE3inputReader != null) {
                                TE3inputReader.close();
                            }
                        }
                    }
                    break;

                case CONVERT_TML:{
                    String convert_to = getParameter(action_parameters, "convert_to");
                    if (convert_to == null || !convert_to.matches("((?i)te3input|isotml|from_isotml)")) {
                        throw new Exception("convert_to parameter is required (te3input,isotml, or from_isotml).");
                    }

                    for (int i = 0; i < input_files.length; i++) {
                        XMLFile nlpfile = new XMLFile(input_files[i],null);
                        if (!nlpfile.getClass().getSimpleName().equals("XMLFile")) {
                            throw new Exception("TIPSem requires XMLFile files as input. Found: " + nlpfile.getClass().getSimpleName());
                        }
                        if (convert_to.equalsIgnoreCase("from_isotml")) {
                            if (!nlpfile.getExtension().equalsIgnoreCase("isotml")) {
                                nlpfile.overrideExtension("isotml");
                            }
                            if (!nlpfile.isWellFormatted()) {
                                throw new Exception("File: " + nlpfile.getFile() + " is not a valid TimeML (.tml) XML file.");
                            }
                            TML_file_utils.ISOTML2TML(nlpfile.getFile().getCanonicalPath());
                        } else {
                            if (!nlpfile.getExtension().equalsIgnoreCase("tml")) {
                                nlpfile.overrideExtension("tml");
                            }
                            if (!nlpfile.isWellFormatted()) {
                                throw new Exception("File: " + nlpfile.getFile() + " is not a valid TimeML (.tml) XML file.");
                            }
                            if (convert_to.equalsIgnoreCase("te3input")) {
                                TML_file_utils.TML2TE3(nlpfile.getFile().getCanonicalPath());
                            } else {
                                TML_file_utils.TML2ISOTML(nlpfile.getFile().getCanonicalPath());
                            }
                        }

                    }
                }
                    break;

                case PLAIN2TE3:{
                    for (int i = 0; i < input_files.length; i++) {
                        PlainFile nlpfile = new PlainFile(input_files[i]);
                        if (!nlpfile.getClass().getSimpleName().equals("PlainFile")) {
                            throw new Exception("TIPSem requires PlainFile files as input. Found: " + nlpfile.getClass().getSimpleName());
                        }
                        TML_file_utils.Plain2TE3(nlpfile.getFile().getCanonicalPath());
                    }
                }
                    break;

                    
                /*case TMLDIR_2_TE2FORMAT: {
                }*/


                case ANNOTATE: {
                    ArrayList<NLPFile> nlp_files = new ArrayList<NLPFile>();
                    if (input_files != null) {
                        //System.err.println("File/s to annotate: " + input_files.length);

                        for (int i = 0; i < input_files.length; i++) {
                            // Check files - exist/encoding
                            File f = new File(input_files[i]);
                            if (!f.exists()) {
                                System.err.println("Searching and downloading from Wikipedia...");
                                String wikifile = Wiki_bk.wiki2txt(input_files[i], lang);
                                if (wikifile != null) {
                                    f = null;
                                    f = new File(wikifile);
                                } else {
                                    throw new FileNotFoundException("File does not exist neither locally nor in Wikipedia: " + f);
                                }
                            }

                            if (!f.isFile()) {
                                throw new IllegalArgumentException("Must be a regular file: " + f);
                            }

                            NLPFile nlpfile;

                            nlpfile = (NLPFile) new PlainFile(f.getAbsolutePath());

                            if (lang == null) {
                                System.err.println("Info: Language not set, using default: en");
                                lang="en";
                                //lang = nlpfile.detectLanguage();
                            }
                            nlpfile.setLanguage(lang);
                            nlp_files.add(nlpfile);
                            f=null;
                        }
                    }
                    if (nlp_files.size() <= 0) {
                        throw new Exception("No input files found");
                    }

                    String approach = getParameter(action_parameters, "approach");
                    if (approach == null) {
                        approach = "TIPSemB";
                    }

                    String entities = getParameter(action_parameters, "entities");
                    if (entities != null){
                        entities=entities.replaceAll("\\s+", "");
                        if(!entities.matches("(timex|event|tlink|tlink-rel-only|timex;event|event;timex|timex;event;tlink|event;timex;tlink)[;]?")) {
                            throw new Exception("entities must follow (timex|event|tlink|tlink-rel-only|timex;event|event;timex|timex;event;tlink|event;timex;tlink)[;]? pattern. Found: " + entities + ".");
                        }
                    }else{
                        entities="timex;event;tlink";
                    }

                    String inputf = getParameter(action_parameters, "inputf");
                    if (inputf != null && !entities.matches("(tlink|tlink-rel-only)")){
                        inputf=inputf.replaceAll("\\s+", "").toLowerCase();
                        if(!(inputf.equals("te3input") || inputf.equals("plain")))
                        throw new Exception("inputf must be plain (default) or te3input. Found: " + inputf + ".");
                    }
                    if (inputf == null) {
                        if(!entities.matches("(tlink|tlink-rel-only)"))
                            inputf = "plain";
                        else
                            inputf = "te3input-with-entities"; // just for informative purposes
                    }

                    String dctvalue = getParameter(action_parameters, "dct");

                    // consider null DCT for history domain in the future... not now.

                    for (NLPFile nlpfile : nlp_files) {
                        System.err.println("\n\nFile: " + nlpfile.getFile() + " Language:" + nlpfile.getLanguage());
                        String output=null;
                        if(!entities.matches("(tlink|tlink-rel-only)"))
                            output=TIP.annotate(nlpfile, inputf, approach, lang, entities, dctvalue,null);
                        else
                            output=TIP.annotate_links(nlpfile, approach, lang, null, null);

                        // OUTPUT FILES
                        (new File(output)).renameTo(new File(output.substring(0, output.lastIndexOf("_" + approach + "_features" + File.separator)) + ".tml"));
                        //(new File(dir + "/" + nlpfile.getFile().getName())).renameTo(nlpfile.getFile());
                    }
                }
                break;

                case ANNOTATECRF: {
                    ArrayList<NLPFile> nlp_files = new ArrayList<NLPFile>();
                    if (input_files != null) {
                        //System.err.println("File/s to annotate: " + input_files.length);

                        for (int i = 0; i < input_files.length; i++) {
                            // Check files - exist/encoding
                            File f = new File(input_files[i]);
                            if (!f.exists()) {
                                System.err.println("Searching and downloading from Wikipedia...");
                                String wikifile = Wiki_bk.wiki2txt(input_files[i], lang);
                                if (wikifile != null) {
                                    f = null;
                                    f = new File(wikifile);
                                } else {
                                    throw new FileNotFoundException("File does not exist neither locally nor in Wikipedia: " + f);
                                }
                            }

                            if (!f.isFile()) {
                                throw new IllegalArgumentException("Must be a regular file: " + f);
                            }

                            NLPFile nlpfile;

                            nlpfile = (NLPFile) new PlainFile(f.getAbsolutePath());

                            if (lang == null) {
                                System.err.println("Info: Language not set, using default: en");
                                lang="en";
                                //lang = nlpfile.detectLanguage();
                            }
                            nlpfile.setLanguage(lang);
                            nlp_files.add(nlpfile);
                        }
                    }
                    if (nlp_files.size() <= 0) {
                        throw new Exception("No input files found");
                    }

                    String approach = getParameter(action_parameters, "approach");
                    if (approach == null) {
                        approach = "TIPSemB";
                    }

                    String entities = getParameter(action_parameters, "entities");
                    if (entities != null){
                        entities=entities.replaceAll("\\s+", "");
                        if(!entities.matches("(timex|event|timex;event|event;timex|timex;event;tlink|event;timex;tlink)[;]?")) {
                            throw new Exception("entities must follow (timex|event|timex;event|event;timex|timex;event;tlink|event;timex;tlink)[;]? pattern. Found: " + entities + ".");
                        }
                    }else{
                        entities="timex;event;tlink";
                    }

                    String inputf = getParameter(action_parameters, "inputf");
                    if (inputf != null){
                        inputf=inputf.replaceAll("\\s+", "").toLowerCase();
                        if(!(inputf.equals("te3input") || inputf.equals("plain")))
                        throw new Exception("inputf must be plain (default) or te3input. Found: " + inputf + ".");
                    }
                    if (inputf == null) {
                        inputf = "plain";
                    }

                    String dctvalue = getParameter(action_parameters, "dct");

                    // consider null DCT for history domain in the future... not now.

                    for (NLPFile nlpfile : nlp_files) {
                        System.err.println("\n\nFile: " + nlpfile.getFile() + " Language:" + nlpfile.getLanguage());
                        String output=TIP.annotateCRF(nlpfile, inputf, approach, lang, entities, dctvalue,null);
                        // OUTPUT FILES
                        System.err.println(output);
                        (new File(output)).renameTo(new File(output.substring(0, output.lastIndexOf("_" + approach + "_features" + File.separator)) + ".tml"));
                        //(new File(dir + "/" + nlpfile.getFile().getName())).renameTo(nlpfile.getFile());

                    }
                }
                break;




















                case TAN:
                    ArrayList<NLPFile> nlp_files = new ArrayList<NLPFile>();
                    if (input_files != null) {
                        //System.err.println("File/s to annotate: " + input_files.length);

                        for (int i = 0; i < input_files.length; i++) {
                            // Check files - exist/encoding
                            File f = new File(input_files[i]);
                            if (!f.exists()) {
                                System.err.println("Searching and downloading from Wikipedia...");
                                String wikifile = Wiki_bk.wiki2txt(input_files[i], lang);
                                if (wikifile != null) {
                                    f = null;
                                    f = new File(wikifile);
                                } else {
                                    throw new FileNotFoundException("File does not exist neither locally nor in Wikipedia: " + f);
                                }
                            }

                            if (!f.isFile()) {
                                throw new IllegalArgumentException("Must be a regular file: " + f);
                            }

                            NLPFile nlpfile;
                            nlpfile = (NLPFile) new PlainFile(f.getAbsolutePath());
                            if (lang == null) {
                                System.err.println("Info: Language not set, using default: en");
                                lang="en";
                                //lang = nlpfile.detectLanguage();
                            }
                            nlpfile.setLanguage(lang);
                            nlp_files.add(nlpfile);
                        }
                    }
                    if (nlp_files.size() <= 0) {
                        throw new Exception("No input files found");
                    }

                    for (NLPFile nlpfile : nlp_files) {
                        if (!nlpfile.getClass().getSimpleName().equals("PlainFile")) {
                            throw new Exception("TIPSem requires PlainFile files as input. Found: " + nlpfile.getClass().getSimpleName());
                        }

                        String approach = getParameter(action_parameters, "approach");
                        if (approach == null) {
                            //throw new Exception("Model filename (model) undefined");
                            approach = "TIPSemB";
                        }

                        String dctvalue = getParameter(action_parameters, "dct");
                        Timex dct = null;
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

                        String ommit_current_implicit = getParameter(action_parameters, "ommitcurrent");
                        String ommit_re = getParameter(action_parameters, "ommit_re");

                        System.err.println("File: " + nlpfile.getFile() + " Language:" + nlpfile.getLanguage() + " omitcurrent: " + ommit_current_implicit);


                        File dir = new File(nlpfile.getFile().getCanonicalPath()  + "_" + approach + "_features" + File.separator);
                        if (!dir.exists() || !dir.isDirectory()) {
                            dir.mkdir();
                        }

                        String output = dir + "/" + nlpfile.getFile().getName();
                        FileUtils.copyFileUtil(nlpfile.getFile(), new File(output));

                        // Write dct.tab
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

                        if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                            System.err.print("Recognizing TIMEX3s");
                        }
                        String timex = CRF.test(features, approach + "_rec_timex_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                        PipesFile nlpfile_temp = new PipesFile(timex);
                        ((PipesFile) nlpfile_temp).isWellFormedOptimist();
                        timex = PipesFile.IOB2check(nlpfile_temp);


                        if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                            System.err.print("Classifying TIMEX3s");
                        }
                        output = Classification.get_classik(timex, lang);
                        String timex_class = CRF.test(output, approach + "_class_timex_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");


                        if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                            System.err.print("Normalizing TIMEX3s (DCT=" + dct.get_value() + ")");
                        }
                        output = TimexNormalization.getTIMEN(timex, timex_class, lang);
                        output = CRF.test(output, approach + "_timen_timex_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                        String timex_norm = TimexNormalization.get_normalized_values(output, lang);

                        output = TempEvalFiles.merge_classik(timex, timex_class, "type");
                        String timex_merged = BaseTokenFeatures.merge_classik_append(output, timex_norm, "value");



                        if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                            System.err.print("Recognizing EVENTs");
                        }
                        String event = CRF.test(features, approach + "_rec_event_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");
                        nlpfile_temp = new PipesFile(event);
                        ((PipesFile) nlpfile_temp).isWellFormedOptimist();
                        event = PipesFile.IOB2check(nlpfile_temp);


                        if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                            System.err.print("Classifying EVENTs");
                        }
                        output = Classification.get_classik(event, lang);
                        String event_class = CRF.test(output, approach + "_class_event_" + nlpfile.getLanguage().toUpperCase() + ".CRFmodel");

                        String event_merged = TempEvalFiles.merge_classik(event, event_class, "class");

                        String all_merged = PipesFile.merge_pipes(timex_merged, event_merged);

                        all_merged = BaseTokenFeatures.putids(all_merged);

                        PipesFile pf = new PipesFile(all_merged);
                        pf.isWellFormedOptimist();

                        HashMap<String, Timex> DCTs = new HashMap<String, Timex>();
                        if (dct != null) {
                            DCTs.put(nlpfile.getFile().getName(), dct);
                        }
                        HashMap<String, Timex> timexes = new HashMap<String, Timex>();
                        HashMap<String, Event> events = new HashMap<String, Event>();
                        HashMap<String,Link> links = new HashMap<String,Link>();

                        timexes.put("t0", new Timex("t0", dct.get_value(), "DATE", dct.get_value(), "-", -1, -1));
                        ElementFiller.get_elements_old(pf, timexes, events, links, ommit_re);

                        // generate possible links / features

                        // categorize them


                        if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                            System.err.println("Generating TML");
                        }

                        // convert links into file-links Hash
                        HashMap<String, HashMap<String,Link>> linksHash = new HashMap<String, HashMap<String,Link>>();
                        linksHash.put(nlpfile.getFile().getName(), links);
                        // TML output: add links to the output
                        output = FileConverter.pipes2tml(pf, DCTs, null, linksHash);



                        // TAn output (XML): conteo, ordenation,...
                        // FAKE TAN


                        // Timexes: x
                        // References
                        // Date
                        // Time
                        // Durations
                        // Sets
                        // Events: y
                        // Occurrences
                        //...

                        // Distribution
                        // Reference a: n events
                        // Reference b: m events
                        // ...


                        // TAn a representación pictonica, html+jgraphs, o gd...

                        // Navegació frases:
                        // Events + importants x referencies + importants (+ separades, amb + events)
                        // Click -> events de la referència + referencies colindants

                        // OUTPUT FILES
                        (new File(output)).renameTo(new File(output.substring(0, output.lastIndexOf("_" + approach + "_features" + File.separator)) + ".tml"));
                        (new File(dir + "/" + nlpfile.getFile().getName())).renameTo(nlpfile.getFile());


                        if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                            System.err.println("Getting TAn");
                        }


                        TAn.fake_TAn(timexes, events, links, nlpfile.getFile().getAbsolutePath(), dct.get_value(), ommit_current_implicit);

                        output = TAn.pipes2navphp(pf);
                        (new File(output)).renameTo(new File(output.substring(0, output.lastIndexOf("_" + approach + "_features" + File.separator)) + ".nav.php"));

                        BaseTokenFeatures.clean(dir + File.separator);

                        /*System.err.print("Executing for " + output);
                        output = CRF.test(output, eventmodel);
                        System.err.println(" saving output in " + output);*/


                        // falta abrir output i convertir a xml (fusionando las columnas...)
                        // si es con classificación normalización, etc.. -> varios ficheros...
                    }
                    break;







                /*case TAB_NFOLD: {
                // Only consider the first file
                if (!(FileUtils.getNLPFormat(nlp_files.get(0).getFile())).equalsIgnoreCase("Tab")) {
                throw new Exception("A TempEval2-like base-segmentation.tab file is required as input to get the folder structure.");
                }

                // GENERATE THE train1, ... trainN
                // test1, ... testN
                String include_test = getParameter(action_parameters, "include_test");
                boolean itest = true;
                if (include_test == null || include_test.equalsIgnoreCase("false")) {
                itest = false;
                }
                int nfolds = 10;
                try {
                String folds = getParameter(action_parameters, "folds");
                nfolds = Integer.parseInt(folds);
                } catch (Exception e) {
                nfolds = 10;
                }

                TempEvalFiles.divide_nfolds(nlp_files.get(0).getFile(), nfolds, itest);
                }
                break;*/

                case TML_NFOLD: {
                    double percentage_size_margin = 0.2;
                    String nfolds = getParameter(action_parameters, "n");
                    if (nfolds == null) {
                        nfolds = "5";
                    }
                    int n = Integer.parseInt(nfolds);
                    if (n < 1) {
                        throw new Exception("The number of folds (n) must be greater than 0.");
                    }
                    if (input_files.length != 1) {
                        throw new Exception("One and only one directory containing tml files is expected.");
                    }
                    File dir = new File(input_files[0]);
                    if (!dir.isDirectory()) {
                        throw new Exception("The input must be a directory.");
                    }

                    File[] tmlfiles = dir.listFiles(FileUtils.onlyFilesFilter);
                    Arrays.sort(tmlfiles, FileUtils.fileSizeDesc);
                    // estaria bien que si ya estan procesadas (.d) copie tb los directorios donde corresponda

                    if (n > tmlfiles.length) {
                        throw new Exception("The number of folds (n) must be lower or equal than the number of files (" + tmlfiles.length + ").");
                    }
                    int total_size = 0;
                    ArrayList<File> farr = new ArrayList<File>();
                    for (int i = 0; i < tmlfiles.length; i++) {
                        total_size += tmlfiles[i].length();
                        farr.add(tmlfiles[i]);
                    }
                    int fold_size = total_size / n;
                    int fold_min_size = (int) ((int) fold_size * percentage_size_margin);
                    ArrayList<ArrayList<String>> folds = new ArrayList<ArrayList<String>>();
                    ArrayList<String> fold = new ArrayList<String>();
                    int current_fold_size = 0;
                    //int last_size=farr.size();
                    while (farr.size() > 0) {
                        for (int i = 0; i < farr.size(); i++) {
                            if (current_fold_size < fold_size) {
                                if (fold.size() == 0 || current_fold_size + farr.get(i).length() < fold_size) {
                                    fold.add(farr.get(i).getCanonicalPath());
                                    current_fold_size += farr.get(i).length();
                                    farr.remove(i);
                                }
                            }
                        }
                        if (current_fold_size < fold_min_size) {
                            throw new Exception("Impossible folding... with this percentage margin: " + percentage_size_margin);
                        } else {
                            folds.add(fold);
                            fold = null;
                            fold = new ArrayList<String>();
                            current_fold_size = 0;
                        }
                        /*if(farr.size()==last_size){
                        throw new Exception("Impossible folding... with this percentage margin: "+percentage_size_margin);
                        }else{
                        last_size=farr.size();
                        }*/
                    }

                    System.out.println(folds);


                    /*input must be the dir with the tmlfiles to nfold

                    output must be just in the same path below one dir... nameof the dir_numfold

                    This will contain the combinations of folds*/

                    // GENERATE THE train1, ... trainN
                    // test1, ... testN

                    /*make array file/size (count total size)(sort by size)
                    split size into n parts.
                    then start creating groups by the bigger files until they reach the fileseze
                    if a group reaches the size in more than the size of the smallest file
                    then use the smallest file before

                    when the groups are created then with a for bucle generate the Nx2 folders
                    with the proper tml_files inside

                    then use tml_training tml_testing 10 fold...*/
                }
                break;


                /*                case TEMPEVAL_RECOGNITION_10FOLD: {
                String retrain = getParameter(action_parameters, "retrain");
                String mlmethod = getParameter(action_parameters, "ML");
                if (mlmethod == null) {
                mlmethod = "CRF";
                }
                String approach = getParameter(action_parameters, "approach");
                if (approach == null) {
                approach = "TIPSem";
                }
                String element = getParameter(action_parameters, "element");
                if (element == null) {
                element = "timex";
                }
                String baseline = getParameter(action_parameters, "baseline");
                if (baseline == null) {
                baseline = "TIPSemB";
                }
                String feature_vector = getParameter(action_parameters, "feature_vector");
                if (feature_vector == null) {
                feature_vector = "TempEval2-features";
                }

                train_model.Recognition_10fold(element.toLowerCase(), approach, lang.toUpperCase(), mlmethod, baseline, retrain, feature_vector);
                }
                break;

                case TEMPEVAL_CLASSIFICATION_10FOLD: {
                String retrain = getParameter(action_parameters, "retrain");
                String mlmethod = getParameter(action_parameters, "ML");
                if (mlmethod == null) {
                mlmethod = "CRF";
                }
                String approach = getParameter(action_parameters, "approach");
                if (approach == null) {
                approach = "TIPSem";
                }
                String element = getParameter(action_parameters, "element");
                if (element == null) {
                element = "timex";
                }
                String baseline = getParameter(action_parameters, "baseline");
                if (baseline == null) {
                baseline = "TIPSemB";
                }
                String feature_vector = getParameter(action_parameters, "feature_vector");
                if (feature_vector == null) {
                feature_vector = "TempEval2-features";
                }

                train_model.Classification_10fold(element.toLowerCase(), approach, lang.toUpperCase(), mlmethod, baseline, retrain, feature_vector);
                }
                break;
                 */


                case TRAIN_AND_TEST: {
                    if(lang==null){
                        lang="en";
                    }
                    String mlmethod = getParameter(action_parameters, "ML");
                    if (mlmethod == null) {
                        mlmethod = "CRF";
                    }
                    String approach = getParameter(action_parameters, "approach");
                    if (approach == null) {
                        approach = "TIPSemB";
                    }
                    String task = getParameter(action_parameters, "task");
                    if (task == null) {
                        task = "recognition";
                    }
                    String element = getParameter(action_parameters, "element");
                    if (element == null) {
                        element = "timex";
                    }
                    String strategy = getParameter(action_parameters, "strategy");
                    if (strategy == null) {
                        strategy = "normal";
                    }
                    String rebuild_dataset = getParameter(action_parameters, "rebuild_dataset");
                    if (rebuild_dataset == null) {
                        rebuild_dataset = "false";
                    }
                    String traind = getParameter(action_parameters, "train_dir");
                    File train_dir = null;
                    if (traind == null) {
                        train_dir = new File(localDatasetPath + lang.toUpperCase() + "/train_tml");
                    } else {
                        train_dir = new File(traind);
                        if (!train_dir.exists() || !train_dir.isDirectory()) {
                            throw new Exception("Input " + traind + " does not exist or is not a directory.");
                        }
                    }
                    String testd = getParameter(action_parameters, "test_dir");
                    File test_dir = null;
                    if (testd == null) {
                        test_dir = new File(localDatasetPath + lang.toUpperCase() + "/test_tml");
                    } else {
                        test_dir = new File(testd);
                        if (!test_dir.exists() || !test_dir.isDirectory()) {
                            throw new Exception("Input " + testd + " does not exist or is not a directory.");
                        }
                    }
                    
                    if(task.equalsIgnoreCase("recognition")) train_model.Recognition_tml(train_dir,test_dir,element.toLowerCase(), approach, lang.toUpperCase(), mlmethod, rebuild_dataset);
                    if(task.equalsIgnoreCase("classification")) train_model.Classification_tml(train_dir,test_dir,element.toLowerCase(), approach, lang.toUpperCase(), mlmethod, rebuild_dataset);
                    if(task.equalsIgnoreCase("normalization")) train_model.NormalizationType_tml(train_dir,test_dir,"timex", approach, lang.toUpperCase(), mlmethod, rebuild_dataset);
                    if(task.equalsIgnoreCase("categorization")) train_model.Categorization_tml(train_dir,test_dir,element.toLowerCase(), approach, lang.toUpperCase(), mlmethod, rebuild_dataset);
                    if(task.equalsIgnoreCase("all")) train_model.full_tml(train_dir,test_dir, approach, lang.toUpperCase(), rebuild_dataset);
                    // LINKS PAPER (use golden entities)
                    if(task.equalsIgnoreCase("idcat")) train_model.idcat_tml(train_dir,test_dir, approach, lang.toUpperCase(), rebuild_dataset, strategy);
                }
                break;


                case TEST_NORMALIZATION_TML: {
                    if(lang==null){
                        lang="en";
                    }
                    String approach = getParameter(action_parameters, "approach");
                    if (approach == null) {
                        approach = "TIPSemB";
                    }
                    String rebuild_dataset = getParameter(action_parameters, "rebuild_dataset");
                    if (rebuild_dataset == null) {
                        rebuild_dataset = "false";
                    }
                    String testd = getParameter(action_parameters, "test_dir");
                    File test_dir = null;
                    if (testd == null) {
                        test_dir = new File(localDatasetPath + lang.toUpperCase() + "/test_tml");
                    } else {
                        test_dir = new File(testd);
                        if (!test_dir.exists() || !test_dir.isDirectory()) {
                            throw new Exception("Input " + testd + " does not exist or is not a directory.");
                        }
                    }
                    train_model.Normalization_tml(test_dir,"timex", approach, lang.toUpperCase(), rebuild_dataset);
                }
                break;





                case DATASET2TML: {
                    if (input_files.length != 1) {
                        throw new Exception("One and only one base-segmentation.tab file is expected.");
                    }
                    File f = new File(input_files[0]);
                    if (!f.exists() || !f.isFile() || f.length()<1) {
                        throw new Exception("The input must be a non-empty file.");
                    }
                        // Obtain a piped equivalent
                        TabFile tabfile = new TabFile(f.getAbsolutePath());
                        tabfile.setLanguage(lang);
                        if (!tabfile.isWellFormatted()) {
                            throw new Exception("A well-formed TempEval2-like base-segmentation.tab file is required as input.");
                        }
                        String base = (tabfile).getPipesFile();
                        base = FileUtils.renameTo(base, "\\.tab\\.pipes", "\\.TempEval-bs");



                        HashMap<String, Timex> DCTs = FileConverter.getTimexDCTsFromTab(f.getCanonicalPath().substring(0, f.getCanonicalPath().lastIndexOf("/")) + "/dct.tab");


                        // Merge all features and attribs (check everything is OK)

                        //timexes
                        PipesFile pipesfile = new PipesFile(base);
                        pipesfile.isWellFormedOptimist();
                        String timex = TempEvalFiles.merge_extents_and_attribs(pipesfile, "timex");

                        //events
                        pipesfile = new PipesFile(base);
                        pipesfile.isWellFormedOptimist();
                        String event = TempEvalFiles.merge_extents_and_attribs(pipesfile, "event");

                        String all_merged = PipesFile.merge_pipes(timex, event);


                        // add links to a hash per file
                        HashMap<String, HashMap<String, Event>> makeinstances = new HashMap<String, HashMap<String, Event>>();
                        HashMap<String, HashMap<String,Link>> links = new HashMap<String, HashMap<String,Link>>(); // et, e-dct (main and reporting), main (prev sent or last main), sub (intra sent)

                        // Expcting no inconsistencies...
                        ElementFiller.get_mk_and_links_from_tab_event_timex(tabfile.getFile().getParent()+"/tlinks-timex-event.tab", makeinstances, links);
                        ElementFiller.get_mk_and_links_from_tab_event_timex(tabfile.getFile().getParent()+"/tlinks-dct-event.tab", makeinstances, links);
                        // Wait for the data...
                        //ElementFiller.get_mk_and_links_from_tab_event_event(tabfile.getFile().getParent()+"/tlinks-main-events.tab", makeinstances, links);
                        //ElementFiller.get_mk_and_links_from_tab_event_event(tabfile.getFile().getParent()+"/tlinks-subordinated-events.tab", makeinstances, links);



                        pipesfile = null;
                        pipesfile = new PipesFile(all_merged);
                        pipesfile.setLanguage(lang);
                        pipesfile.isWellFormedOptimist();
                        FileConverter.pipes2tml(pipesfile, DCTs, makeinstances,links);

                        // Create a working directory
                        File newdir = new File(tabfile.getFile().getParent() + "/tml-files/");
                        if (!newdir.exists() || !newdir.isDirectory()) {
                            newdir.mkdir();
                        }

                        File olddir = new File(tabfile.getFile().getParent());
                        File[] tmlfiles = olddir.listFiles(new FilenameFilter() {
                            public boolean accept(File dir, String name) {
                                return name.endsWith(".tml");

                            }
                        });

                        for (int i=0;i<tmlfiles.length;i++){
                            if(!tmlfiles[i].renameTo(new File(newdir, tmlfiles[i].getName()))){
                                throw new Exception("Error moving the TML files.");
                            }
                        }

                        // Copy the file
//                        String output = dir + "/" + nlpfile.getFile().getName();
//                        FileUtils.copyFileUtil(nlpfile.getFile(), new File(output));
                        // it would be needed to pass extents and attribs files...

                    
                }
                break;


                case RECOGNITION2TML: {
                    if (input_files.length != 1) {
                        throw new Exception("One and only one base-segmentation.tab file is expected.");
                    }
                    File f = new File(input_files[0]);
                    if (!f.exists() || !f.isFile() || f.length()<1) {
                        throw new Exception("The input must be a non-empty file.");
                    }
                        // Obtain a piped equivalent
                        PipesFile pipesfile = new PipesFile(f.getAbsolutePath());
                        pipesfile.setLanguage(lang);
                        if (!pipesfile.isWellFormedOptimist()) {
                            throw new Exception("A well-formed TempEval2-like file is required as input.");
                        }

                        FileConverter.pipes2tml(pipesfile, null, null,null);

                        // Create a working directory
                        File newdir = new File(pipesfile.getFile().getParent() + "/tml-files/");
                        if (!newdir.exists() || !newdir.isDirectory()) {
                            newdir.mkdir();
                        }

                        File olddir = new File(pipesfile.getFile().getParent());
                        File[] tmlfiles = olddir.listFiles(new FilenameFilter() {
                            public boolean accept(File dir, String name) {
                                return name.endsWith(".tml");

                            }
                        });

                        for (int i=0;i<tmlfiles.length;i++){
                            if(!tmlfiles[i].renameTo(new File(newdir, tmlfiles[i].getName()))){
                                throw new Exception("Error moving the TML files.");
                            }
                        }

                        // Copy the file
//                        String output = dir + "/" + nlpfile.getFile().getName();
//                        FileUtils.copyFileUtil(nlpfile.getFile(), new File(output));
                        // it would be needed to pass extents and attribs files...


                }
                break;




                /*
                case TRAIN_RECOGNITION: {
                String retrain = getParameter(action_parameters, "retrain");
                String mlmethod = getParameter(action_parameters, "ML");
                if (mlmethod == null) {
                mlmethod = "CRF";
                }
                String approach = getParameter(action_parameters, "approach");
                if (approach == null) {
                approach = "TIPSemB";
                }
                String element = getParameter(action_parameters, "element");
                if (element == null) {
                element = "timex";
                }
                String baseline = getParameter(action_parameters, "baseline");
                if (baseline == null) {
                baseline = "TIPSem_nosr";
                }
                train_model.Recognition(element.toLowerCase(), approach, lang.toUpperCase(), mlmethod, baseline, retrain);
                }
                break;

                case TRAIN_CLASSIFICATION: {
                String retrain = getParameter(action_parameters, "retrain");
                String mlmethod = getParameter(action_parameters, "ML");
                if (mlmethod == null) {
                mlmethod = "CRF";
                }
                String approach = getParameter(action_parameters, "approach");
                if (approach == null) {
                approach = "TIPSemB";
                }
                String element = getParameter(action_parameters, "element");
                if (element == null) {
                element = "timex";
                }
                String baseline = getParameter(action_parameters, "baseline");
                if (baseline == null) {
                baseline = "TIPSem_nosr";
                }
                train_model.Classification(element.toLowerCase(), approach, lang.toUpperCase(), mlmethod, baseline, retrain);
                }
                break;


                case TRAIN_NORMALIZATIONTYPE: {
                String retrain = getParameter(action_parameters, "retrain");
                String mlmethod = getParameter(action_parameters, "ML");
                if (mlmethod == null) {
                mlmethod = "CRF";
                }
                String approach = getParameter(action_parameters, "approach");
                if (approach == null) {
                approach = "TIPSemB";
                }
                String baseline = getParameter(action_parameters, "baseline");
                if (baseline == null) {
                baseline = "TIPSem_nosr";
                }
                train_model.NormalizationType("timex", approach, lang.toUpperCase(), mlmethod, baseline, retrain);
                }
                break;


                case TEST_NORMALIZATION: {
                String approach = getParameter(action_parameters, "approach");
                if (approach == null) {
                approach = "TIPSemB";
                }
                String baseline = getParameter(action_parameters, "baseline");
                train_model.Normalization("timex", approach, lang.toUpperCase(), baseline);
                }
                break;




                case TRAIN_CATEGORIZATION: {
                String retrain = getParameter(action_parameters, "retrain");
                String mlmethod = getParameter(action_parameters, "ML");
                if (mlmethod == null) {
                mlmethod = "CRF";
                }
                String approach = getParameter(action_parameters, "approach");
                if (approach == null) {
                approach = "TIPSemB";
                }
                String element = getParameter(action_parameters, "element");
                if (element == null) {
                element = "TASKC";
                }
                String baseline = getParameter(action_parameters, "baseline");
                if (baseline == null) {
                baseline = "TIPSem_nosr";
                }
                train_model.Categorization(element.toUpperCase(), approach, lang.toUpperCase(), mlmethod, baseline, retrain);
                }
                break;

                 */

            }
        } catch (Exception e) {
            System.err.println("\nErrors found (OptionHandler):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
            //throw new Exception("\tAction: " + action.toUpperCase());
        }

    }
}
