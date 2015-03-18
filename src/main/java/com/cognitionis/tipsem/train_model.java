package com.cognitionis.tipsem;

/**
 * train_model.java
 * @author Hector Llorens
 * @since Sep 6, 2010, 14:08:35 PM
 *
 * ONLY TO BE DISTRIBUTED IN ADMINS VERSION
 * THE REGULAR VERSION WILL HAVE THE PRE-TRAINED MODELS
 *
 */
import com.cognitionis.external_tools.*;
import com.cognitionis.feature_builder.*;
import com.cognitionis.utils_basickit.FileUtils;
import com.cognitionis.nlp_files.TempEvalFiles;
import com.cognitionis.nlp_files.XMLFile;
import com.cognitionis.nlp_files.PipesFile;
import com.cognitionis.nlp_files.annotation_scorers.*;
import com.cognitionis.timeml_basickit.TML_file_utils;
import java.io.*;
import java.util.*;

public class train_model {

    public static String localDatasetPath = FileUtils.getApplicationPath() + "program-data/TIMEE-training/";
    public static HashMap<String, String> category_files = new HashMap<String, String>() {

        {
            put("e-t", "base-segmentation.e-t-link-features");
            put("e-dct", "base-segmentation.e-dct-link-features");
            put("e-main", "base-segmentation.e-main-link-features");
            put("e-sub", "base-segmentation.e-sub-link-features");
        }
    };

    public static void Recognition_tml(File train_dir, File test_dir, String elem, String approach, String lang, String method, String re_build_dataset) {
        String output = "";
        PipesFile nlpfile;
        Scorer scorer = new Scorer();
        try {
            File dir = new File(train_dir.getParent() + File.separator + "experiments_tml" + File.separator + approach + File.separator);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                    throw new Exception("Directory not created...");
                }
            }
            // Check for features files (train/test)
            if (re_build_dataset.equalsIgnoreCase("true") || !(new File(train_dir.getParent() + File.separator + train_dir.getName() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features")).exists()) {
                FileConverter.tmldir2features(train_dir, approach, lang);
            }
            if (re_build_dataset.equalsIgnoreCase("true") || !(new File(test_dir.getParent() + File.separator + test_dir.getName() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features")).exists()) {
                FileConverter.tmldir2features(test_dir, approach, lang);
            }

            String model = dir + File.separator + approach + "_rec_" + elem + "_" + lang + "." + method + "model";
            System.out.println("model: " + model);
            output = TempEvalFiles.merge_extents(train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-extents.tab", elem);

            if (method.equals("CRF")) {
                output = CRF.train(output, approach + "_rec_" + elem + ".template");
            }
            if (method.equals("SVM")) {
                output = SVM.train(output, approach + "_rec_" + elem + ".template");
            }
            (new File(output)).renameTo(new File(model));
            //(new File(output)).renameTo(new File((new File(output)).getCanonicalPath().substring((new File(output)).getName().indexOf(approach))));
            //test Model
            // Hacer opcional por parametro...
            //getFeatures(lang,"test/entities");
            System.out.println("Test..." + model);
            if (method.equals("CRF")) {
                output = CRF.test(test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", model);
            }
            if (method.equals("SVM")) {
                output = SVM.test(test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", model);
            }
            nlpfile = new PipesFile(output);
            ((PipesFile) nlpfile).isWellFormedOptimist();
            String temp = PipesFile.IOB2check(nlpfile);
            (new File(temp)).renameTo(new File(output));

            String annot = dir + File.separator + (new File(output)).getName();
            (new File(output)).renameTo(new File(annot));
            nlpfile = new PipesFile(annot);
            ((PipesFile) nlpfile).isWellFormedOptimist();

            output = TempEvalFiles.merge_extents(test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-extents.tab", elem);
            (new File(output)).renameTo(new File(annot + "-key"));


            // TempEvalFiles-2 results
            System.out.println("Results: " + approach);
            //TempEval_scorer.score_entities(extents, TempEvalpath +lang+"/test/entities/"+ elem + "-attributes.tab", lang, elem);

            // AnnotScore results
            Score score = scorer.score(nlpfile, annot + "-key", nlpfile.getColumn("element\\(IOB2\\)"), -1);
            //score.print("attribs");
            score.print("detail");
            //score.print(printopts);
            //score.print("");


        } catch (Exception e) {
            System.err.println("Errors found (Experimenter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    public static void Classification_tml(File train_dir, File test_dir, String elem, String approach, String lang, String method, String re_build_dataset) {
        String output = "", key;
        Scorer scorer = new Scorer();
        try {
            File dir = new File(train_dir.getParent() + File.separator + "experiments_tml" + File.separator + approach + File.separator);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                    throw new Exception("Directory not created...");
                }
            }

            // Check for features files (train/test)
            if (re_build_dataset.equalsIgnoreCase("true") || !(new File(train_dir.getParent() + File.separator + train_dir.getName() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features")).exists()) {
                FileConverter.tmldir2features(train_dir, approach, lang);
            }
            if (re_build_dataset.equalsIgnoreCase("true") || !(new File(test_dir.getParent() + File.separator + test_dir.getName() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features")).exists()) {
                FileConverter.tmldir2features(test_dir, approach, lang);
            }


            String model = dir + File.separator + approach + "_class_" + elem + "_" + lang + "." + method + "model";

            output = TempEvalFiles.merge_extents(train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-extents.tab", elem);
            output = TempEvalFiles.merge_attribs(train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features-annotationKey-" + elem, train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-attributes.tab", elem);
            output = Classification.get_classik(output, lang);


            if (method.equals("CRF")) {
                output = CRF.train(output, approach + "_class_" + elem + ".template");
            }
            if (method.equals("SVM")) {
                output = SVM.train(output, approach + "_class_" + elem + ".template");
            }
            (new File(output)).renameTo(new File(model));
            //(new File(output)).renameTo(new File((new File(output)).getCanonicalPath().substring((new File(output)).getName().indexOf(approach))));
            //test Model
            // Hacer opcional por parametro...
            //getFeatures(lang,"test/entities");
            //System.out.println("Test...");
            output = TempEvalFiles.merge_extents(test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-extents.tab", elem);
            output = Classification.get_classik(output, lang);

            if (method.equals("CRF")) {
                output = CRF.test(output, model);
            }
            if (method.equals("SVM")) {
                output = SVM.test(output, model);
            }

            String annot = dir + File.separator + (new File(output)).getName();
            (new File(output)).renameTo(new File(annot));
            /*PipesFile nlpannot = new PipesFile();
            nlpannot.loadFile(new File(annot));
            ((PipesFile) nlpannot).isWellFormedOptimist();*/


            key = TempEvalFiles.merge_extents(test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-extents.tab", elem);
            key = TempEvalFiles.merge_attribs(test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features-annotationKey-" + elem, test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-attributes.tab", elem);
            key = Classification.get_classik(key, lang);


            // TempEvalFiles-2 results
            System.out.println("Results: " + approach);
            //TempEval_scorer.score_entities(extents, TempEvalpath +lang+"/test/entities/"+ elem + "-attributes.tab", lang, elem);

            // AnnotScore results
            Score score = scorer.score_class(annot, key, -1);
            //score.print("attribs");
            //score.print("detail");
            //score.print(printopts);
            score.print("");

        } catch (Exception e) {
            System.err.println("Errors found (Experimenter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    public static void NormalizationType_tml(File train_dir, File test_dir, String elem, String approach, String lang, String method, String re_build_dataset) {
        String output = "", key;
        Scorer scorer = new Scorer();
        try {
            File dir = new File(train_dir.getParent() + File.separator + "experiments_tml" + File.separator + approach + File.separator);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                    throw new Exception("Directory not created...");
                }
            }
            // Check for features files (train/test)
            if (re_build_dataset.equalsIgnoreCase("true") || !(new File(train_dir.getParent() + File.separator + train_dir.getName() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features")).exists()) {
                FileConverter.tmldir2features(train_dir, approach, lang);
            }
            if (re_build_dataset.equalsIgnoreCase("true") || !(new File(test_dir.getParent() + File.separator + test_dir.getName() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features")).exists()) {
                FileConverter.tmldir2features(test_dir, approach, lang);
            }

            String model = dir + File.separator + approach + "_timen_" + elem + "_" + lang + "." + method + "model";
            output = TempEvalFiles.merge_extents(train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-extents.tab", elem);
            String features = TempEvalFiles.merge_attribs(train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features-annotationKey-" + elem, train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-attributes.tab", elem);
            output = Classification.get_classik(features, lang);
            output = TimexNormalization.getTIMEN(features, output, lang);

            if (method.equals("CRF")) {
                output = CRF.train(output, approach + "_timen_" + elem + ".template");
            }
            if (method.equals("SVM")) {
                output = SVM.train(output, approach + "_timen_" + elem + ".template");
            }
            (new File(output)).renameTo(new File(model));



            features = TempEvalFiles.merge_extents(test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-extents.tab", elem);
            output = Classification.get_classik(features, lang);
            output = TimexNormalization.getTIMEN(features, output, lang);

            if (method.equals("CRF")) {
                output = CRF.test(output, model);
            }
            if (method.equals("SVM")) {
                output = SVM.test(output, model);
            }

            String annot = dir + File.separator + (new File(output)).getName();
            (new File(output)).renameTo(new File(annot));

            key = TempEvalFiles.merge_extents(test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-extents.tab", elem);
            String keyfeatures = TempEvalFiles.merge_attribs(test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features-annotationKey-" + elem, test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-attributes.tab", elem);
            key = Classification.get_classik(keyfeatures, lang);
            key = TimexNormalization.getTIMEN(keyfeatures, key, lang);


            // TempEvalFiles-2 results
            System.out.println("Results: " + approach);
            //TempEval_scorer.score_entities(extents, TempEvalpath +lang+"/test/entities/"+ elem + "-attributes.tab", lang, elem);
            // AnnotScore results
            Score score = scorer.score_class(annot, key, -1);
            score.print("");


        } catch (Exception e) {
            System.err.println("Errors found (Experimenter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    public static void Categorization_tml(File train_dir, File test_dir, String elem, String approach, String lang, String ml_method, String re_build_dataset) {
        String output = "", key;
        Scorer scorer = new Scorer();
        try {
            if (!elem.matches("e-(t|dct|main|sub)")) {
                throw new Exception("elem must match:e-(t|dct|main|sub). Found: " + elem);
            }

            File dir = new File(train_dir.getParent() + File.separator + "experiments_tml" + File.separator + approach + File.separator);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                    throw new Exception("Directory not created...");
                }
            }
            // Check for features files (train/test)
            if (re_build_dataset.equalsIgnoreCase("true") || !(new File(train_dir.getParent() + File.separator + train_dir.getName() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features")).exists()) {
                FileConverter.tmldir2features(train_dir, approach, lang);
            }
            if (re_build_dataset.equalsIgnoreCase("true") || !(new File(test_dir.getParent() + File.separator + test_dir.getName() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features")).exists()) {
                FileConverter.tmldir2features(test_dir, approach, lang);
            }

            String model = dir + File.separator + approach + "_categ_" + elem + "_" + lang + "." + ml_method + "model";
            output = train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + category_files.get(elem) + "-annotationKey";
            if (ml_method.equals("CRF")) {
                output = CRF.train(output, approach + "_categ_" + elem + ".template");
            }
            if (ml_method.equals("SVM")) {
                output = SVM.train(output, approach + "_categ_" + elem + ".template");
            }
            (new File(output)).renameTo(new File(model));


            output = test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + category_files.get(elem);

            if (ml_method.equals("CRF")) {
                output = CRF.test(output, model);
            }
            if (ml_method.equals("SVM")) {
                output = SVM.test(output, model);
            }

            String annot = dir + File.separator + (new File(output)).getName();
            (new File(output)).renameTo(new File(annot));
            /*PipesFile nlpannot = new PipesFile();
            nlpannot.loadFile(new File(annot));
            ((PipesFile) nlpannot).isWellFormedOptimist();
             */
            key = test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + category_files.get(elem) + "-annotationKey";


            // TempEvalFiles-2 results
            System.out.println("Results: " + approach);
            //TempEval_scorer.score_entities(extents, TempEvalpath +lang+"/test/entities/"+ elem + "-attributes.tab", lang, elem);

            // AnnotScore results
            Score score = scorer.score_class(annot, key, -1);
            //score.print("attribs");
            //score.print("detail");
            //score.print(printopts);
            score.print("");

        } catch (Exception e) {
            System.err.println("Errors found (Experimenter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }

    }

    // test normalization...
    public static void Normalization_tml(File test_dir, String elem, String approach, String lang, String re_build_dataset) {
        String output = "", key;
        Scorer scorer = new Scorer();
        try {
            File dir = new File(test_dir.getParent() + File.separator + "experiments_tml" + File.separator + approach + File.separator);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                    throw new Exception("Directory not created...");
                }
            }
            // Check for features files (train/test)
            if (re_build_dataset.equalsIgnoreCase("true") || !(new File(test_dir.getParent() + File.separator + test_dir.getName() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features")).exists()) {
                FileConverter.tmldir2features(test_dir, approach, lang);
            }
            output = TempEvalFiles.merge_extents(test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-extents.tab", elem);
            String features = TempEvalFiles.merge_attribs(test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features-annotationKey-" + elem, test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-attributes.tab", elem);
            output = Classification.get_classik(features, lang);
            output = TimexNormalization.getTIMEN(features, output, lang);
            output = TimexNormalization.get_normalized_values(output, lang);
            System.out.println(output);
            String annot = dir + File.separator + (new File(output)).getName();
            (new File(output)).renameTo(new File(annot));
            key = TempEvalFiles.merge_extents(test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-extents.tab", elem);
            String keyfeatures = TempEvalFiles.merge_attribs(test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features-annotationKey-" + elem, test_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + elem + "-attributes.tab", elem);
            key = Classification.get_classik(keyfeatures, lang);
            key = TimexNormalization.getTIMEN(keyfeatures, key, lang);
            key = TimexNormalization.get_key_normalized_values(key);
            // TempEvalFiles-2 results
            System.out.println("Testset Results: " + approach);
            //TempEval_scorer.score_entities(extents, TempEvalpath +lang+"/test/entities/"+ elem + "-attributes.tab", lang, elem);
            // AnnotScore results
            Score score = scorer.score_class(annot, key, -1);
            score.print("detail");
        } catch (Exception e) {
            System.err.println("Errors found (Experimenter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    public static void full_tml(File train_dir, File test_dir, String approach, String lang, String re_build_dataset) {
        String output = "";
        try {
            File dir_trainmodels = new File(train_dir.getCanonicalPath() + "-models-" + approach + File.separator);
            if (!dir_trainmodels.exists()) {
                if (!dir_trainmodels.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                    throw new Exception("Directory not created...");
                }
            }

            File dir_test_annotation = new File(test_dir.getCanonicalPath() + "-" + approach + File.separator);
            if (!dir_test_annotation.exists()) {
                if (!dir_test_annotation.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                    throw new Exception("Directory not created...");
                }
            }

            File dir_test_te3input = new File(test_dir.getCanonicalPath() + "-input-" + approach + File.separator);
            if (!dir_test_te3input.exists()) {
                if (!dir_test_te3input.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                    throw new Exception("Directory not created...");
                }
            }

            // Check for features files (train/test)
            if (re_build_dataset.equalsIgnoreCase("true") || !(new File(train_dir.getParent() + File.separator + train_dir.getName() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features")).exists()) {
                FileConverter.tmldir2features(train_dir, approach, lang);
            }
            /*if (re_build_dataset.equalsIgnoreCase("true") || !(new File(test_dir.getParent() + File.separator + test_dir.getName() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features")).exists()) {
            FileConverter.tmldir2features(test_dir, approach, lang);
            }*/

            String model = dir_trainmodels + File.separator + approach + "_rec_timex_" + lang + ".CRFmodel";
            // check if already trained
            if (!(new File(model)).exists()) {
                //timex
                System.out.println("model: " + model);
                output = TempEvalFiles.merge_extents(train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "timex-extents.tab", "timex");
                output = CRF.train(output, approach + "_rec_timex.template");
                (new File(output)).renameTo(new File(model));

                model = dir_trainmodels + File.separator + approach + "_class_timex_" + lang + ".SVMmodel";
                output = TempEvalFiles.merge_extents(train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "timex-extents.tab", "timex");
                output = TempEvalFiles.merge_attribs(train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features-annotationKey-timex", train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "timex-attributes.tab", "timex");
                output = Classification.get_classik(output, lang);
                output = SVM.train(output, approach + "_class_timex.template");
                (new File(output)).renameTo(new File(model));
                    
                model = dir_trainmodels + File.separator + approach + "_timen_timex_" + lang + ".SVMmodel";
                output = TempEvalFiles.merge_extents(train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "timex-extents.tab", "timex");
                String features = TempEvalFiles.merge_attribs(train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features-annotationKey-timex", train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "timex-attributes.tab", "timex");
                output = Classification.get_classik(features, lang);
                output = TimexNormalization.getTIMEN(features, output, lang);
                output = SVM.train(output, approach + "_timen_timex.template");
                (new File(output)).renameTo(new File(model));


                // event
                model = dir_trainmodels + File.separator + approach + "_rec_event_" + lang + ".CRFmodel";
                System.out.println("model: " + model);
                output = TempEvalFiles.merge_extents(train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "event-extents.tab", "event");
                output = CRF.train(output, approach + "_rec_event.template");
                (new File(output)).renameTo(new File(model));

                model = dir_trainmodels + File.separator + approach + "_class_event_" + lang + ".SVMmodel";
                output = TempEvalFiles.merge_extents(train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features", train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "event-extents.tab", "event");
                output = TempEvalFiles.merge_attribs(train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features-annotationKey-event", train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + "event-attributes.tab", "event");
                output = Classification.get_classik(output, lang);
                output = SVM.train(output, approach + "_class_event.template");
                (new File(output)).renameTo(new File(model));

                // links
                model = dir_trainmodels + File.separator + approach + "_categ_e-t_" + lang + ".SVMmodel";
                output = train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + category_files.get("e-t") + "-annotationKey";
                output = SVM.train(output, approach + "_categ_e-t.template");
                (new File(output)).renameTo(new File(model));

                model = dir_trainmodels + File.separator + approach + "_categ_e-dct_" + lang + ".SVMmodel";
                output = train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + category_files.get("e-dct") + "-annotationKey";
                output = SVM.train(output, approach + "_categ_e-dct.template");
                (new File(output)).renameTo(new File(model));

                model = dir_trainmodels + File.separator + approach + "_categ_e-main_" + lang + ".CRFmodel";
                output = train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + category_files.get("e-main") + "-annotationKey";
                output = CRF.train(output, approach + "_categ_e-main.template");
                (new File(output)).renameTo(new File(model));

                model = dir_trainmodels + File.separator + approach + "_categ_e-sub_" + lang + ".CRFmodel";
                output = train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + category_files.get("e-sub") + "-annotationKey";
                output = CRF.train(output, approach + "_categ_e-sub.template");
                (new File(output)).renameTo(new File(model));
            }

            // test
            File[] tmlfiles = test_dir.listFiles(FileUtils.onlyFilesFilter);
            for (int i = 0; i < tmlfiles.length; i++) {
                XMLFile nlpfile = new XMLFile(tmlfiles[i].getAbsolutePath(),null);
                if (!nlpfile.getExtension().equalsIgnoreCase("tml")) {
                    nlpfile.overrideExtension("tml");
                }
                if (!nlpfile.isWellFormatted()) {
                    throw new Exception("File: " + nlpfile.getFile() + " is not a valid TimeML (.tml) XML file.");
                }
                String te3input = TML_file_utils.TML2TE3(nlpfile.getFile().getCanonicalPath());
                String basefile = dir_test_te3input + File.separator + new File(te3input).getName();
                (new File(te3input)).renameTo(new File(basefile));
                nlpfile=new XMLFile(basefile,null);
                nlpfile.setLanguage(lang);
                output = TIP.annotate(nlpfile, "te3input", approach, lang, null, null, dir_trainmodels.getCanonicalPath());
                (new File(output)).renameTo(new File(dir_test_annotation + File.separator + tmlfiles[i].getName()));
            }

        } catch (Exception e) {
            System.err.println("Errors found (Experimenter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    public static void idcat_tml(File train_dir, File test_dir, String approach, String lang, String re_build_dataset, String strategy) {
        String output = "";
        try {
            File dir_trainmodels = new File(train_dir.getCanonicalPath() + "-models-" + approach + File.separator);
            if (!dir_trainmodels.exists()) {
                if (!dir_trainmodels.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                    throw new Exception("Directory not created...");
                }
            }

            File dir_test_annotation = new File(test_dir.getCanonicalPath() + "-" + approach + "-links-"+ strategy + File.separator);
            if (!dir_test_annotation.exists()) {
                if (!dir_test_annotation.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                    throw new Exception("Directory not created...");
                }
            }

            File dir_test_te3input = new File(test_dir.getCanonicalPath() + "-input4links-" + approach + File.separator);
            if (!dir_test_te3input.exists()) {
                if (!dir_test_te3input.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                    throw new Exception("Directory not created...");
                }
            }

            // Check for features files (train/test)
            if (re_build_dataset.equalsIgnoreCase("true") || !(new File(train_dir.getParent() + File.separator + train_dir.getName() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features")).exists()) {
                FileConverter.tmldir2features(train_dir, approach, lang);
            }
            /*if (re_build_dataset.equalsIgnoreCase("true") || !(new File(test_dir.getParent() + File.separator + test_dir.getName() + "_" + approach + "_features" + File.separator + "base-segmentation.TempEval2-features")).exists()) {
            FileConverter.tmldir2features(test_dir, approach, lang);
            }*/

            String model = dir_trainmodels + File.separator + approach + "_categ_e-t_" + lang + ".SVMmodel";
            // check if already trained
            if (!(new File(model)).exists() && !strategy.equalsIgnoreCase("super-baseline")) {
                // links
                model = dir_trainmodels + File.separator + approach + "_categ_e-t_" + lang + ".SVMmodel";
                output = train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + category_files.get("e-t") + "-annotationKey";
                output = SVM.train(output, approach + "_categ_e-t.template");
                (new File(output)).renameTo(new File(model));

                model = dir_trainmodels + File.separator + approach + "_categ_e-dct_" + lang + ".SVMmodel";
                output = train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + category_files.get("e-dct") + "-annotationKey";
                output = SVM.train(output, approach + "_categ_e-dct.template");
                (new File(output)).renameTo(new File(model));

                model = dir_trainmodels + File.separator + approach + "_categ_e-main_" + lang + ".CRFmodel";
                output = train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + category_files.get("e-main") + "-annotationKey";
                output = CRF.train(output, approach + "_categ_e-main.template");
                (new File(output)).renameTo(new File(model));

                model = dir_trainmodels + File.separator + approach + "_categ_e-sub_" + lang + ".CRFmodel";
                output = train_dir.getCanonicalPath() + "_" + approach + "_features" + File.separator + category_files.get("e-sub") + "-annotationKey";
                output = CRF.train(output, approach + "_categ_e-sub.template");
                (new File(output)).renameTo(new File(model));
            }

            // test
            File[] tmlfiles = test_dir.listFiles(FileUtils.onlyFilesFilter);
            for (int i = 0; i < tmlfiles.length; i++) {
                XMLFile nlpfile = new XMLFile(tmlfiles[i].getAbsolutePath(),null);
                if (!nlpfile.getExtension().equalsIgnoreCase("tml")) {
                    nlpfile.overrideExtension("tml");
                }
                if (!nlpfile.isWellFormatted()) {
                    throw new Exception("File: " + nlpfile.getFile() + " is not a valid TimeML (.tml) XML file.");
                }
                String onlyEntitiesInput = TML_file_utils.TML2onlyEntities(nlpfile.getFile().getCanonicalPath());
                String basefile = dir_test_te3input + File.separator + new File(onlyEntitiesInput).getName();
                (new File(onlyEntitiesInput)).renameTo(new File(basefile));
                nlpfile=new XMLFile(basefile,null);
                nlpfile.setLanguage(lang);
                output = TIP.annotate_links(nlpfile, approach, lang, dir_trainmodels.getCanonicalPath(),strategy);
                (new File(output)).renameTo(new File(dir_test_annotation + File.separator + tmlfiles[i].getName()));
            }

        } catch (Exception e) {
            System.err.println("Errors found (Experimenter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

}
