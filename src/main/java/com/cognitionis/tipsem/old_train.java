package com.cognitionis.tipsem;

import com.cognitionis.external_tools.*;
import com.cognitionis.feature_builder.*;
import com.cognitionis.utils_basickit.FileUtils;
import com.cognitionis.utils_basickit.StringUtils;
import com.cognitionis.nlp_files.TempEvalFiles;
import com.cognitionis.nlp_files.PipesFile;
import com.cognitionis.nlp_files.annotation_scorers.*;
import com.cognitionis.utils_basickit.statistics.T_test;
import java.io.*;
import java.util.*;

/**
 * @author Hector Llorens
 * @since 2011
 */
public class old_train {

            public static String localDatasetPath = FileUtils.getApplicationPath() +"program-data/TIMEE-training/";


    public static HashMap<String, String> category_files = new HashMap<String, String>() {
        {
            put("TASKC", "tlinks-timex-event.tab");
            put("TASKD", "tlinks-dct-event.tab");
            put("TASKE", "tlinks-main-events.tab");
            put("TASKF", "tlinks-subordinated-events.tab");
        }
    };


//    poner los keys en los corpus y no rehacer si ya estan hechos (no en TIPSEM (APPROACH))
    public static void Recognition(String elem, String approach, String lang, String method) {
        Recognition(elem, approach, lang, method, null, null);
    }

    public static void Recognition(String elem, String approach, String lang, String method, String baseline, String retrain) {
        String output = "";
        PipesFile nlpfile;
        Scorer scorer = new Scorer();
        try {
            File dir = new File(localDatasetPath + "experiments/" + approach + "/");
            if (!dir.exists()) {
                if (!dir.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                    throw new Exception("Directory not created...");
                }
            }

            // Check for features files (train/test)
            if (!(new File(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features")).exists()) {
                BaseTokenFeatures.getFeatures4Tab(lang, localDatasetPath + lang + "/train/base-segmentation.tab", "TempEval2-features", approach);
            }
            if (!(new File(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features")).exists()) {
                BaseTokenFeatures.getFeatures4Tab(lang, localDatasetPath + lang + "/test/entities/base-segmentation.tab", "TempEval2-features", approach);
            }


            String model = dir + "/" + approach + "_rec_" + elem + "_" + lang + "." + method + "model";
            System.out.println("model: " + model);
            if (!(new File(model)).exists() || retrain != null) {
                output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features", localDatasetPath + lang + "/train/" + elem + "-extents.tab", elem);

                if (method.equals("CRF")) {
                    output = CRF.train(output, approach + "_rec_" + elem + ".template");
                }
                if (method.equals("SVM")) {
                    output = SVM.train(output, approach + "_rec_" + elem + ".template");
                }
                (new File(output)).renameTo(new File(model));
                //(new File(output)).renameTo(new File((new File(output)).getCanonicalPath().substring((new File(output)).getName().indexOf(approach))));
            }
            //test Model
            // Hacer opcional por parametro...
            //getFeatures(lang,"test/entities");
            System.out.println("Test..." + model);
            if (method.equals("CRF")) {
                output = CRF.test(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", model);
            }
            if (method.equals("SVM")) {
                output = SVM.test(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", model);
            }
            nlpfile = new PipesFile(output);
            ((PipesFile) nlpfile).isWellFormedOptimist();
            String temp = PipesFile.IOB2check(nlpfile);
            (new File(temp)).renameTo(new File(output));

            String annot = dir + "/" + (new File(output)).getName();
            (new File(output)).renameTo(new File(annot));
            nlpfile = new PipesFile(annot);
            ((PipesFile) nlpfile).isWellFormedOptimist();

            output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test/entities/" + elem + "-extents.tab", elem);
            (new File(output)).renameTo(new File(annot + "-key"));


            // TempEvalFiles-2 results
            System.out.println("Results: " + approach);
            //TempEval_scorer.score_entities(extents, TempEvalpath +lang+"/test/entities/"+ elem + "-attributes.tab", lang, elem);

            // AnnotScore results
            Score score = scorer.score(nlpfile, annot + "-key", nlpfile.getColumn("element\\(IOB2\\)"), -1);
            //score.print("attribs");
            //score.print("detail");
            //score.print(printopts);
            score.print("");


            if (baseline != null) {
                File baselinef = null;
                if (method.equals("CRF")) {
                    baselinef = new File(CRF.program_path + baseline + "_rec_" + elem + ".template");
                }
                if (method.equals("SVM")) {
                    baselinef = new File(SVM.program_path + baseline + "_rec_" + elem + ".template");
                }


                if (baselinef.exists()) {
                    model = dir + "/" + baseline + "_rec_" + elem + "_" + lang + "." + method + "model";
                    if (!(new File(model)).exists() || retrain != null) {

                        output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features", localDatasetPath + lang + "/train/" + elem + "-extents.tab", elem);
                        if (method.equals("CRF")) {
                            output = CRF.train(output, baseline + "_rec_" + elem + ".template");
                        }
                        if (method.equals("SVM")) {
                            output = SVM.train(output, baseline + "_rec_" + elem + ".template");
                        }

                        (new File(output)).renameTo(new File(model));
                    }




                    if (method.equals("CRF")) {
                        output = CRF.test(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", model);
                    }
                    if (method.equals("SVM")) {
                        output = SVM.test(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", model);
                    }


                    nlpfile = new PipesFile(output);
                    ((PipesFile) nlpfile).isWellFormedOptimist();
                    temp = PipesFile.IOB2check(nlpfile);
                    (new File(temp)).renameTo(new File(output));

                    String annot_nosr = dir + "/" + (new File(output)).getName();

                    (new File(output)).renameTo(new File(annot_nosr));
                    nlpfile = new PipesFile(annot_nosr);
                    ((PipesFile) nlpfile).isWellFormedOptimist();
                    (new File(output)).renameTo(new File(annot_nosr));

                    output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test/entities/" + elem + "-extents.tab", elem);


                    System.out.println("Results: " + baseline);
                    Score score2 = scorer.score(nlpfile, annot + "-key", nlpfile.getColumn("element\\(IOB2\\)"), -1);
                    score2.print("");



                    // EN QUE MEJORAN LOS ROLES?
                    scorer.compare_scores(score, score2);


                }
            }


        } catch (Exception e) {
            System.err.println("Errors found (Experimenter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

        public static void Categorization(String elem, String approach, String lang, String method) {
        Categorization(elem, approach, lang, method, null, null);
    }

    public static void Categorization(String elem, String approach, String lang, String method, String baseline, String retrain) {

        String output = "", key;
        Scorer scorer = new Scorer();
        try {
            File dir = new File(localDatasetPath + "experiments/" + approach + "/");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Check for features files (train/test)
            if (!(new File(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features")).exists()) {
                BaseTokenFeatures.getFeatures4Tab(lang, localDatasetPath + lang + "/train/base-segmentation.tab", "TempEval2-features", approach);
            }
            if (!(new File(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features")).exists()) {
                BaseTokenFeatures.getFeatures4Tab(lang, localDatasetPath + lang + "/test/relations/base-segmentation.tab", "TempEval2-features", approach);
            }

            String model = dir + "/" + approach + "_categ_" + elem + "_" + lang + "." + method + "model";
            if (!(new File(model)).exists() || retrain != null) {
                output = CategorizationTE2.get_categorization(localDatasetPath + lang + "/train/" + category_files.get(elem), elem);
                if (method.equals("CRF")) {
                    output = CRF.train(output, approach + "_categ_" + elem + ".template");
                }
                if (method.equals("SVM")) {
                    output = SVM.train(output, approach + "_categ_" + elem + ".template");
                }
                (new File(output)).renameTo(new File(model));
            }


            output = CategorizationTE2.get_categorization(localDatasetPath + lang + "/test/relations/" + category_files.get(elem), elem);

            if (method.equals("CRF")) {
                output = CRF.test(output, model);
            }
            if (method.equals("SVM")) {
                output = SVM.test(output, model);
            }

            String annot = dir + "/" + (new File(output)).getName();
            (new File(output)).renameTo(new File(annot));
            /*PipesFile nlpannot = new PipesFile();
            nlpannot.loadFile(new File(annot));
            ((PipesFile) nlpannot).isWellFormedOptimist();
             */
            key = CategorizationTE2.get_categorization(localDatasetPath + lang + "/test/relations/" + category_files.get(elem), elem);


            // TempEvalFiles-2 results
            System.out.println("Results: " + approach);
            //TempEval_scorer.score_entities(extents, TempEvalpath +lang+"/test/entities/"+ elem + "-attributes.tab", lang, elem);

            // AnnotScore results
            Score score = scorer.score_class(annot, key, -1);
            //score.print("attribs");
            //score.print("detail");
            //score.print(printopts);
            score.print("");


            if (baseline != null) {
                File baselinef = null;
                if (method.equals("CRF")) {
                    baselinef = new File(CRF.program_path + baseline + "_categ_" + elem + ".template");
                }
                if (method.equals("SVM")) {
                    baselinef = new File(SVM.program_path + baseline + "_categ_" + elem + ".template");
                }


                if (baselinef.exists()) {
                    model = dir + "/" + baseline + "_categ_" + elem + "_" + lang + "." + method + "model";
                    if (!(new File(model)).exists() || retrain != null) {
                        output = CategorizationTE2.get_categorization(localDatasetPath + lang + "/train/" + category_files.get(elem), elem);

                        if (method.equals("CRF")) {
                            output = CRF.train(output, baseline + "_categ_" + elem + ".template");
                        }
                        if (method.equals("SVM")) {
                            output = SVM.train(output, baseline + "_categ_" + elem + ".template");
                        }

                        (new File(output)).renameTo(new File(model));
                    }


                    output = CategorizationTE2.get_categorization(localDatasetPath + lang + "/test/relations/" + category_files.get(elem), elem);


                    if (method.equals("CRF")) {
                        output = CRF.test(output, model);
                    }
                    if (method.equals("SVM")) {
                        output = SVM.test(output, model);
                    }
                    String annot_nosr = dir + "/" + (new File(output)).getName();

                    (new File(output)).renameTo(new File(annot_nosr));
                    /*PipesFile nlpannot_nosr = new PipesFile();
                    nlpannot_nosr.loadFile(new File(annot_nosr));
                    ((PipesFile) nlpannot_nosr).isWellFormedOptimist();
                     */



                    // Build key
                    key = CategorizationTE2.get_categorization(localDatasetPath + lang + "/test/relations/" + category_files.get(elem), elem);


                    System.out.println("Results: " + baseline);
                    Score score2 = scorer.score_class(annot_nosr, key, -1);
                    score2.print("");



                    // EN QUE MEJORAN LOS ROLES?
                    scorer.compare_scores(score, score2);


                }
            }
        } catch (Exception e) {
            System.err.println("Errors found (Experimenter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }

    }
    
    public static void Recognition_10fold(String elem, String approach, String lang, String method, String baseline, String retrain, String feature_vector) {
        String output = "";
        PipesFile nlpfile;
        Scorer scorer = new Scorer();
        ArrayList<Score> approach_scores = new ArrayList<Score>();
        ArrayList<Score> baseline_scores = new ArrayList<Score>();
        try {
            File dir = new File(localDatasetPath + "experiments/" + approach + "/");
            if (!dir.exists()) {
                if (!dir.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                    throw new Exception("Directory not created...");
                }
            }




            for (int n = 1; n <= 10; n++) {
                File dir2 = new File(dir + "/" + "fold" + n + "/");
                if (!dir2.exists()) {
                    if (!dir2.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                        throw new Exception("Directory not created...");
                    }
                }
                System.out.println("\n\nFOLD " + n + "--------------\n\n");

                // Check for features files (train/test)
                if (!(new File(localDatasetPath + lang + "/train" + n + "/base-segmentation." + feature_vector)).exists()) {
                    BaseTokenFeatures.getFeatures4Tab(lang, localDatasetPath + lang + "/train" + n + "/base-segmentation.tab", feature_vector, "TIPSem");
                }
                if (!(new File(localDatasetPath + lang + "/test" + n + "/base-segmentation." + feature_vector)).exists()) {
                    BaseTokenFeatures.getFeatures4Tab(lang, localDatasetPath + lang + "/test" + n + "/base-segmentation.tab", feature_vector, "TIPSem");
                }


                String model = dir + "/" + "fold" + n + "/" + approach + "_rec_" + elem + "_" + lang + "." + method + "model";
                System.out.println("model: " + model);
                if (!(new File(model)).exists() || retrain != null) {
                    output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/train" + n + "/base-segmentation." + feature_vector, localDatasetPath + lang + "/train" + n + "/" + elem + "-extents.tab", elem);

                    if (method.equals("CRF")) {
                        output = CRF.train(output, approach + "_rec_" + elem + ".template");
                    }
                    if (method.equals("SVM")) {
                        output = SVM.train(output, approach + "_rec_" + elem + ".template");
                    }
                    (new File(output)).renameTo(new File(model));
                    //(new File(output)).renameTo(new File((new File(output)).getCanonicalPath().substring((new File(output)).getName().indexOf(approach))));
                }
                //test Model
                // Hacer opcional por parametro...
                System.out.println("Test..." + model);
                if (method.equals("CRF")) {
                    output = CRF.test(localDatasetPath + lang + "/test" + n + "/base-segmentation." + feature_vector, model);
                }
                if (method.equals("SVM")) {
                    output = SVM.test(localDatasetPath + lang + "/test" + n + "/base-segmentation." + feature_vector, model);
                }
                nlpfile = new PipesFile(output);
                ((PipesFile) nlpfile).isWellFormedOptimist();
                String temp = PipesFile.IOB2check(nlpfile);
                (new File(temp)).renameTo(new File(output));

                String annot = dir + "/" + "fold" + n + "/" + (new File(output)).getName();
                (new File(output)).renameTo(new File(annot));
                nlpfile = new PipesFile(annot);
                ((PipesFile) nlpfile).isWellFormedOptimist();

                output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test" + n + "/base-segmentation." + feature_vector, localDatasetPath + lang + "/test" + n + "/" + elem + "-extents.tab", elem);
                (new File(output)).renameTo(new File(annot + "-key"));


                // TempEvalFiles-2 results
                System.out.println("Results: " + approach);
                //TempEval_scorer.score_entities(extents, TempEvalpath +lang+"/test/"+ elem + "-attributes.tab", lang, elem);

                // AnnotScore results
                Score score = scorer.score(nlpfile, annot + "-key", nlpfile.getColumn("element\\(IOB2\\)"), -1);
                //score.print("attribs");
                //score.print("detail");
                //score.print(printopts);
                score.print("");
                approach_scores.add((Score) score.clone());

                if (baseline != null) {
                    File baselinef = null;
                    if (method.equals("CRF")) {
                        baselinef = new File(CRF.program_path + "templates/" + baseline + "_rec_" + elem + ".template");
                    }
                    if (method.equals("SVM")) {
                        baselinef = new File(SVM.program_path + "templates/" + baseline + "_rec_" + elem + ".template");
                    }


                    if (baselinef.exists()) {
                        model = dir + "/" + "fold" + n + "/" + baseline + "_rec_" + elem + "_" + lang + "." + method + "model";
                        if (!(new File(model)).exists() || retrain != null) {

                            output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/train" + n + "/base-segmentation." + feature_vector, localDatasetPath + lang + "/train" + n + "/" + elem + "-extents.tab", elem);
                            if (method.equals("CRF")) {
                                output = CRF.train(output, baseline + "_rec_" + elem + ".template");
                            }
                            if (method.equals("SVM")) {
                                output = SVM.train(output, baseline + "_rec_" + elem + ".template");
                            }

                            (new File(output)).renameTo(new File(model));
                        }




                        if (method.equals("CRF")) {
                            output = CRF.test(localDatasetPath + lang + "/test" + n + "/base-segmentation." + feature_vector, model);
                        }
                        if (method.equals("SVM")) {
                            output = SVM.test(localDatasetPath + lang + "/test" + n + "/base-segmentation." + feature_vector, model);
                        }


                        nlpfile = new PipesFile(output);
                        ((PipesFile) nlpfile).isWellFormedOptimist();
                        temp = PipesFile.IOB2check(nlpfile);
                        (new File(temp)).renameTo(new File(output));

                        String annot_nosr = dir + "/" + "fold" + n + "/" + (new File(output)).getName();

                        (new File(output)).renameTo(new File(annot_nosr));
                        nlpfile = new PipesFile(annot_nosr);
                        ((PipesFile) nlpfile).isWellFormedOptimist();
                        (new File(output)).renameTo(new File(annot_nosr));

                        output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test" + n + "/base-segmentation." + feature_vector, localDatasetPath + lang + "/test" + n + "/" + elem + "-extents.tab", elem);


                        System.out.println("Results: " + baseline);
                        Score score2 = scorer.score(nlpfile, annot + "-key", nlpfile.getColumn("element\\(IOB2\\)"), -1);
                        score2.print("");

                        baseline_scores.add(score2);


                        // EN QUE MEJORAN LOS ROLES?
                        //scorer.compare_scores(score, score2);


                    }
                }
            }






            // Aquí puedo tener 10 pares de scores y calcular si la differencia es significativa
            String element = elem;
            if (element.equals("timex")) {
                element += "3";
            }
            double mean_P = 0.0;
            double mean_R = 0.0;
            double mean_F1 = 0.0;

            if (approach_scores.size() == 10) {
                for (int fold = 0; fold < 10; fold++) {
                    mean_P += approach_scores.get(fold).getPrecisionTokenLevel(element);
                    mean_R += approach_scores.get(fold).getRecallTokenLevel(element);
                    mean_F1 += approach_scores.get(fold).getF1TokenLevel(element);
                }
                mean_P = mean_P / 10.0;
                mean_R = mean_R / 10.0;
                mean_F1 = mean_F1 / 10.0;
                System.out.println("\n\n\n----------------10 Fold (approach: " + approach + ")\n--> precis\trecall\tf1\n--> " + StringUtils.twoDecPosS(mean_P) + " \t& " + StringUtils.twoDecPosS(mean_R) + " \t& " + StringUtils.twoDecPosS(mean_F1));
            }

            if (baseline_scores.size() == 10 && approach_scores.size() == 10) {
                double mean_Pb = 0.0;
                double mean_Rb = 0.0;
                double mean_F1b = 0.0;
                double mean_Pdiff = 0.0;
                double mean_Rdiff = 0.0;
                double mean_F1diff = 0.0;
                double mean_Pimp = 0.0;
                double mean_Rimp = 0.0;
                double mean_F1imp = 0.0;
                double mean_Perr = 0.0;
                double mean_Rerr = 0.0;
                double mean_F1err = 0.0;
                double diffsP[] = new double[10];
                double diffsR[] = new double[10];
                double diffsF1[] = new double[10];
                double impP[] = new double[10];
                double impR[] = new double[10];
                double impF1[] = new double[10];
                double errP[] = new double[10];
                double errR[] = new double[10];
                double errF1[] = new double[10];
                for (int fold = 0; fold < 10; fold++) {
                    diffsP[fold] = approach_scores.get(fold).getPrecisionTokenLevel(element) - baseline_scores.get(fold).getPrecisionTokenLevel(element);
                    diffsR[fold] = approach_scores.get(fold).getRecallTokenLevel(element) - baseline_scores.get(fold).getRecallTokenLevel(element);
                    diffsF1[fold] = approach_scores.get(fold).getF1TokenLevel(element) - baseline_scores.get(fold).getF1TokenLevel(element);

                    impP[fold] = ((approach_scores.get(fold).getPrecisionTokenLevel(element) * 100.0) / baseline_scores.get(fold).getPrecisionTokenLevel(element)) - 100.0;
                    impR[fold] = ((approach_scores.get(fold).getRecallTokenLevel(element) * 100.0) / baseline_scores.get(fold).getRecallTokenLevel(element)) - 100.0;
                    impF1[fold] = ((approach_scores.get(fold).getF1TokenLevel(element) * 100.0) / baseline_scores.get(fold).getF1TokenLevel(element)) - 100.0;

                    errP[fold] = 100 * (((1 - baseline_scores.get(fold).getPrecisionTokenLevel(element)) - (1 - approach_scores.get(fold).getPrecisionTokenLevel(element))) / (1 - baseline_scores.get(fold).getPrecisionTokenLevel(element)));
                    errR[fold] = 100 * (((1 - baseline_scores.get(fold).getRecallTokenLevel(element)) - (1 - approach_scores.get(fold).getRecallTokenLevel(element))) / (1 - baseline_scores.get(fold).getRecallTokenLevel(element)));
                    errF1[fold] = 100 * (((1 - baseline_scores.get(fold).getF1TokenLevel(element)) - (1 - approach_scores.get(fold).getF1TokenLevel(element))) / (1 - baseline_scores.get(fold).getF1TokenLevel(element)));

                    mean_Pb += baseline_scores.get(fold).getPrecisionTokenLevel(element);
                    mean_Rb += baseline_scores.get(fold).getRecallTokenLevel(element);
                    mean_F1b += baseline_scores.get(fold).getF1TokenLevel(element);

                    mean_Pdiff += diffsP[fold];
                    mean_Rdiff += diffsR[fold];
                    mean_F1diff += diffsF1[fold];

                    mean_Pimp += impP[fold];
                    mean_Rimp += impR[fold];
                    mean_F1imp += impF1[fold];

                    mean_Perr += errP[fold];
                    mean_Rerr += errR[fold];
                    mean_F1err += errF1[fold];
                }
                mean_Pb = mean_Pb / 10.0;
                mean_Rb = mean_Rb / 10.0;
                mean_F1b = mean_F1b / 10.0;

                mean_Pdiff = mean_Pdiff / 10.0;
                mean_Rdiff = mean_Rdiff / 10.0;
                mean_F1diff = mean_F1diff / 10.0;

                mean_Pimp = mean_Pimp / 10.0;
                mean_Rimp = mean_Rimp / 10.0;
                mean_F1imp = mean_F1imp / 10.0;

                mean_Perr = mean_Perr / 10.0;
                mean_Rerr = mean_Rerr / 10.0;
                mean_F1err = mean_F1err / 10.0;

                System.out.println("\n----------------10 Fold (baseline: " + baseline + ")\n--> precis \trecall\tf1\n--> " + StringUtils.twoDecPosS(mean_Pb) + " \t& " + StringUtils.twoDecPosS(mean_Rb) + " \t& " + StringUtils.twoDecPosS(mean_F1b));


                System.out.println("\n----------------10 Fold DIFFERENCES\n-->       pre\trec\tf1");
                System.out.println("--> DIFF: " + StringUtils.twoDecPosS(mean_Pdiff) + "  \t& " + StringUtils.twoDecPosS(mean_Rdiff) + "  \t& " + StringUtils.twoDecPosS(mean_F1diff));
                System.out.println("--> IMP: " + StringUtils.twoDecPosS(mean_Pimp) + "  \t& " + StringUtils.twoDecPosS(mean_Rimp) + "  \t& " + StringUtils.twoDecPosS(mean_F1imp));
                System.out.println("--> ERR: " + StringUtils.twoDecPosS(mean_Perr) + "  \t& " + StringUtils.twoDecPosS(mean_Rerr) + "  \t& " + StringUtils.twoDecPosS(mean_F1err));


                System.out.println("\n\ndiffP significance:\n " + T_test.paired_t_test(diffsP) + "");
                System.out.println("diffR significance:\n " + T_test.paired_t_test(diffsR) + "");
                System.out.println("diffF1 significance:\n " + T_test.paired_t_test(diffsF1) + "");

                System.out.println("\n\nimpP significance:\n " + T_test.paired_t_test(impP) + "");
                System.out.println("impR significance:\n " + T_test.paired_t_test(impR) + "");
                System.out.println("impF1 significance:\n " + T_test.paired_t_test(impF1) + "");

                System.out.println("\n\nerrP significance:\n " + T_test.paired_t_test(errP) + "");
                System.out.println("errR significance:\n " + T_test.paired_t_test(errR) + "");
                System.out.println("errF1 significance:\n " + T_test.paired_t_test(errF1) + "");


                boolean latex = true;

                if (latex) {
                    System.out.println("\n\n\\begin{table} [h]\n\\begin{footnotesize}\n\\begin{center}\n\\begin{tabular} {llllllllllll}\n\\hline\\rule{-2pt}{8pt}\n& \\multicolumn{3}{c}{\\textbf{" + baseline + "}} & \\hspace{0.2cm} & \\multicolumn{3}{c}{\\textbf{" + approach + "}}  & \\hspace{0.2cm} & \\multicolumn{3}{c}{\\textbf{Difference}}\\\\");
                    System.out.println("\\textbf{Dataset \\hspace{0.2cm}} \t& \\textbf{P} \t& \\textbf{R} \t& \\textbf{F1} \t& \t& \\textbf{P} \t& \\textbf{R} \t& \\textbf{F1} \t& \t& \\textbf{P} \t& \\textbf{R} \t& \\textbf{F1}\\\\\n  \\hline\\rule{-2pt}{8pt}");
                    for (int fold = 0; fold < 10; fold++) {
                        System.out.println("\\textbf{Fold-" + (fold + 1) + "} \t& " + StringUtils.twoDecPosS(baseline_scores.get(fold).getPrecisionTokenLevel(element)) + " & " + StringUtils.twoDecPosS(baseline_scores.get(fold).getRecallTokenLevel(element)) + " 	\t& \\multicolumn{1}{| >{\\columncolor[rgb]{0.8,0.8,0.8}}l|}{" + StringUtils.twoDecPosS(baseline_scores.get(fold).getF1TokenLevel(element)) + "} \t& \t& " + StringUtils.twoDecPosS(approach_scores.get(fold).getPrecisionTokenLevel(element)) + " \t& " + StringUtils.twoDecPosS(approach_scores.get(fold).getRecallTokenLevel(element)) + " & \\multicolumn{1}{| >{\\columncolor[rgb]{0.8,0.8,0.8}}l|}{" + StringUtils.twoDecPosS(approach_scores.get(fold).getF1TokenLevel(element)) + "} \t& \t& " + StringUtils.twoDecPosS(diffsP[fold]) + " \t& " + StringUtils.twoDecPosS(diffsR[fold]) + " & \\multicolumn{1}{| >{\\columncolor[rgb]{0.8,0.8,0.8}}l|}{" + StringUtils.twoDecPosS(diffsF1[fold]) + "} \\\\");
                    }
                    System.out.println("\\hline\\rule{-2pt}{8pt}");
                    System.out.println("\\textbf{Mean} \t& " + StringUtils.twoDecPosS(mean_Pb) + " \t& " + StringUtils.twoDecPosS(mean_Rb) + " \t& \\multicolumn{1}{| >{\\columncolor[rgb]{0.8,0.8,0.8}}l|}{" + StringUtils.twoDecPosS(mean_F1b) + "} \t& \t& " + StringUtils.twoDecPosS(mean_P) + " \t& " + StringUtils.twoDecPosS(mean_R) + " \t& \\multicolumn{1}{| >{\\columncolor[rgb]{0.8,0.8,0.8}}l|}{" + StringUtils.twoDecPosS(mean_F1) + "} \t& \t& " + StringUtils.twoDecPosS(mean_Pdiff) + " \t& " + StringUtils.twoDecPosS(mean_Rdiff) + " \t& \\multicolumn{1}{| >{\\columncolor[rgb]{0.8,0.8,0.8}}l|}{" + StringUtils.twoDecPosS(mean_F1diff) + "} \\\\");
                    System.out.println("\\hline\n\\end{tabular}\n\\ \\\\\\vspace{0.5cm}\\ \\\\");

                    System.out.println("\\begin{tabular} {lrlll}\n\\hline\\rule{-2pt}{8pt} \\textbf{Comparison} 	 & & & & \\\\\n\\textbf{TIPSem/TIPSem-B} & 	& \\textbf{Mean}  	& \\textbf{SD}	& \\textbf{t (p one-tail)} \\\\");

                    System.out.println("\\hline\\rule{-2pt}{8pt}\\textbf{Difference}");
                    String ttest = T_test.paired_t_test(diffsP);
                    System.out.println("\t& \\textbf{P} \t& " + T_test.latex_t_test(ttest));
                    ttest = T_test.paired_t_test(diffsR);
                    System.out.println("\t& \\textbf{R} \t& " + T_test.latex_t_test(ttest));
                    ttest = T_test.paired_t_test(diffsF1);
                    System.out.println("\t& \\textbf{F1} \t& " + T_test.latex_t_test(ttest));

                    System.out.println("\\hline\\rule{-2pt}{8pt}\\textbf{Improvement \\%}");
                    ttest = T_test.paired_t_test(impP);
                    System.out.println("\t& \\textbf{P} \t& " + T_test.latex_t_test(ttest));
                    ttest = T_test.paired_t_test(impR);
                    System.out.println("\t& \\textbf{R} \t& " + T_test.latex_t_test(ttest));
                    ttest = T_test.paired_t_test(impF1);
                    System.out.println("\t& \\textbf{F1} \t& " + T_test.latex_t_test(ttest));

                    System.out.println("\\hline\\rule{-2pt}{8pt}\\textbf{Relative error}");
                    ttest = T_test.paired_t_test(errP);
                    System.out.println("\t& \\textbf{P} \t& " + T_test.latex_t_test(ttest));
                    ttest = T_test.paired_t_test(errR);
                    System.out.println("\\ \\textbf{reduction \\%} \t& \\textbf{R} \t& " + T_test.latex_t_test(ttest));
                    ttest = T_test.paired_t_test(errF1);
                    System.out.println("\t& \\textbf{F1} \t& " + T_test.latex_t_test(ttest));
                    System.out.println("\\hline\n\\end{tabular}\n\\end{center}\n\\caption{" + elem + " recognition 10-fold (English)}\\label{tab:10-fold-" + elem + "-rec}");
                    System.out.println("\\end{footnotesize}\n\\end{table}");
                }
            }





        } catch (Exception e) {
            System.err.println("Errors found (Experimenter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    public static void Classification(String elem, String approach, String lang, String method) {
        Classification(elem, approach, lang, method, null, null);
    }

    public static void Classification(String elem, String approach, String lang, String method, String baseline, String retrain) {
        String output = "", key;
        Scorer scorer = new Scorer();
        try {
            File dir = new File(localDatasetPath + "experiments/" + approach + "/");
            if (!dir.exists()) {
                dir.mkdirs();
            }


            // Check for features files (train/test)
            if (!(new File(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features")).exists()) {
                BaseTokenFeatures.getFeatures4Tab(lang, localDatasetPath + lang + "/train/base-segmentation.tab", "TempEval2-features", approach);
            }
            if (!(new File(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features")).exists()) {
                BaseTokenFeatures.getFeatures4Tab(lang, localDatasetPath + lang + "/test/entities/base-segmentation.tab", "TempEval2-features", approach);
            }

            String model = dir + "/" + approach + "_class_" + elem + "_" + lang + "." + method + "model";
            if (!(new File(model)).exists() || retrain != null) {

                output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features", localDatasetPath + lang + "/train/" + elem + "-extents.tab", elem);
                output = TempEvalFiles.merge_attribs(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/train/" + elem + "-attributes.tab", elem);
                output = Classification.get_classik(output, lang);


                if (method.equals("CRF")) {
                    output = CRF.train(output, approach + "_class_" + elem + ".template");
                }
                if (method.equals("SVM")) {
                    output = SVM.train(output, approach + "_class_" + elem + ".template");
                }
                (new File(output)).renameTo(new File(model));
                //(new File(output)).renameTo(new File((new File(output)).getCanonicalPath().substring((new File(output)).getName().indexOf(approach))));
            }
            //test Model
            // Hacer opcional por parametro...
            //getFeatures(lang,"test/entities");
            //System.out.println("Test...");



            output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test/entities/" + elem + "-extents.tab", elem);
            output = Classification.get_classik(output, lang);





            if (method.equals("CRF")) {
                output = CRF.test(output, model);
            }
            if (method.equals("SVM")) {
                output = SVM.test(output, model);
            }

            String annot = dir + "/" + (new File(output)).getName();
            (new File(output)).renameTo(new File(annot));
            /*PipesFile nlpannot = new PipesFile();
            nlpannot.loadFile(new File(annot));
            ((PipesFile) nlpannot).isWellFormedOptimist();*/


            key = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test/entities/" + elem + "-extents.tab", elem);
            key = TempEvalFiles.merge_attribs(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/test/entities/" + elem + "-attributes.tab", elem);
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


            if (baseline != null) {
                File baselinef = null;
                if (method.equals("CRF")) {
                    baselinef = new File(CRF.program_path + baseline + "_class_" + elem + ".template");
                }
                if (method.equals("SVM")) {
                    baselinef = new File(SVM.program_path + baseline + "_class_" + elem + ".template");
                }


                if (baselinef.exists()) {
                    model = dir + "/" + baseline + "_class_" + elem + "_" + lang + "." + method + "model";
                    if (!(new File(model)).exists() || retrain != null) {
                        output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features", localDatasetPath + lang + "/train/" + elem + "-extents.tab", elem);
                        output = TempEvalFiles.merge_attribs(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/train/" + elem + "-attributes.tab", elem);
                        output = Classification.get_classik(output, lang);

                        if (method.equals("CRF")) {
                            output = CRF.train(output, baseline + "_class_" + elem + ".template");
                        }
                        if (method.equals("SVM")) {
                            output = SVM.train(output, baseline + "_class_" + elem + ".template");
                        }

                        (new File(output)).renameTo(new File(model));
                    }


                    output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test/entities/" + elem + "-extents.tab", elem);
                    output = Classification.get_classik(output, lang);


                    if (method.equals("CRF")) {
                        output = CRF.test(output, model);
                    }
                    if (method.equals("SVM")) {
                        output = SVM.test(output, model);
                    }
                    String annot_nosr = dir + "/" + (new File(output)).getName();

                    (new File(output)).renameTo(new File(annot_nosr));
                    /*PipesFile nlpannot_nosr = new PipesFile();
                    nlpannot_nosr.loadFile(new File(annot_nosr));
                    ((PipesFile) nlpannot_nosr).isWellFormedOptimist();*/




                    // Build key
                    key = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test/entities/" + elem + "-extents.tab", elem);
                    key = TempEvalFiles.merge_attribs(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/test/entities/" + elem + "-attributes.tab", elem);
                    key = Classification.get_classik(key, lang);


                    System.out.println("Results: " + baseline);
                    Score score2 = scorer.score_class(annot_nosr, key, -1);
                    score2.print("");



                    // EN QUE MEJORAN LOS ROLES?
                    scorer.compare_scores(score, score2);


                }
            }


        } catch (Exception e) {
            System.err.println("Errors found (Experimenter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

        public static void Classification_10fold(String elem, String approach, String lang, String method, String baseline, String retrain, String show) {
        String output = "", key;
        Scorer scorer = new Scorer();
        ArrayList<Score> approach_scores = new ArrayList<Score>();
        ArrayList<Score> baseline_scores = new ArrayList<Score>();
        try {
            File dir = new File(localDatasetPath + "experiments/" + approach + "/");
            if (!dir.exists()) {
                if (!dir.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                    throw new Exception("Directory not created...");
                }
            }




            for (int n = 1; n <= 10; n++) {
                File dir2 = new File(dir + "/" + "fold" + n + "/");
                if (!dir2.exists()) {
                    if (!dir2.mkdirs()) {  // mkdir only creates one, mkdirs creates many parent dirs if needed
                        throw new Exception("Directory not created...");
                    }
                }
                System.out.println("\n\nFOLD " + n + "--------------\n\n");

                // Check for features files (train/test)
                if (!(new File(localDatasetPath + lang + "/train" + n + "/base-segmentation." + "TempEval2-features")).exists()) {
                    BaseTokenFeatures.getFeatures4Tab(lang, localDatasetPath + lang + "/train" + n + "/base-segmentation.tab", "TempEval2-features","TIPSem");
                }
                if (!(new File(localDatasetPath + lang + "/test" + n + "/base-segmentation." + "TempEval2-features")).exists()) {
                    BaseTokenFeatures.getFeatures4Tab(lang, localDatasetPath + lang + "/test" + n + "/base-segmentation.tab", "TempEval2-features","TIPSem");
                }


                String model = dir + "/" + "fold" + n + "/" + approach + "_class_" + elem + "_" + lang + "." + method + "model";



            if (!(new File(model)).exists() || retrain != null) {

                output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/train" + n + "/base-segmentation.TempEval2-features", localDatasetPath + lang + "/train" + n + "/" + elem + "-extents.tab", elem);
                output =  TempEvalFiles.merge_attribs(localDatasetPath + lang + "/train" + n + "/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/train" + n + "/" + elem + "-attributes.tab", elem);
                output = Classification.get_classik(output, lang);


                if (method.equals("CRF")) {
                    output = CRF.train(output, approach + "_class_" + elem + ".template");
                }
                if (method.equals("SVM")) {
                    output = SVM.train(output, approach + "_class_" + elem + ".template");
                }
                (new File(output)).renameTo(new File(model));
                //(new File(output)).renameTo(new File((new File(output)).getCanonicalPath().substring((new File(output)).getName().indexOf(approach))));
            }
            //test Model
            // Hacer opcional por parametro...
            //System.out.println("Test...");



            output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test" + n + "/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test" + n + "/" + elem + "-extents.tab", elem);
            output = Classification.get_classik(output, lang);





            if (method.equals("CRF")) {
                output = CRF.test(output, model);
            }
            if (method.equals("SVM")) {
                output = SVM.test(output, model);
            }

            String annot = dir + "/"  + "fold" + n + "/" + (new File(output)).getName();
            (new File(output)).renameTo(new File(annot));
            /*PipesFile nlpannot = new PipesFile();
            nlpannot.loadFile(new File(annot));
            ((PipesFile) nlpannot).isWellFormedOptimist();*/


            key = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test" + n + "/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test" + n + "/" + elem + "-extents.tab", elem);
            key =  TempEvalFiles.merge_attribs(localDatasetPath + lang + "/test" + n + "/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/test" + n + "/" + elem + "-attributes.tab", elem);
            key = Classification.get_classik(key, lang);







            // TempEvalFiles-2 results
            System.out.println("Results: " + approach);
            //TempEval_scorer.score_entities(extents, TempEvalpath +lang+"/test/entities/"+ elem + "-attributes.tab", lang, elem);

            // AnnotScore results
            Score score = scorer.score_class(annot, key, -1);

            if (show == null || show.equalsIgnoreCase("summary")) {
                score.print("");
            } else {
                if (show.equalsIgnoreCase("detail")) {
                    score.print("detail");
                }
            }

            approach_scores.add((Score) score.clone());


            //score.print("attribs");
            //score.print("detail");
            //score.print(printopts);


            if (baseline != null) {
                File baselinef = null;
                if (method.equals("CRF")) {
                    baselinef = new File(CRF.program_path + "templates/" + baseline + "_class_" + elem + ".template");
                }
                if (method.equals("SVM")) {
                    baselinef = new File(SVM.program_path + "templates/" + baseline + "_class_" + elem + ".template");
                }


                if (baselinef.exists()) {
                    model = dir + "/"  + "fold" + n + "/"+ baseline + "_class_" + elem + "_" + lang + "." + method + "model";
                    if (!(new File(model)).exists() || retrain != null) {
                        output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/train" + n + "/base-segmentation.TempEval2-features", localDatasetPath + lang + "/train" + n + "/" + elem + "-extents.tab", elem);
                        output =  TempEvalFiles.merge_attribs(localDatasetPath + lang + "/train" + n + "/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/train" + n + "/" + elem + "-attributes.tab", elem);
                        output = Classification.get_classik(output, lang);

                        if (method.equals("CRF")) {
                            output = CRF.train(output, baseline + "_class_" + elem + ".template");
                        }
                        if (method.equals("SVM")) {
                            output = SVM.train(output, baseline + "_class_" + elem + ".template");
                        }

                        (new File(output)).renameTo(new File(model));
                    }


                    output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test" + n + "/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test" + n + "/" + elem + "-extents.tab", elem);
                    output = Classification.get_classik(output, lang);


                    if (method.equals("CRF")) {
                        output = CRF.test(output, model);
                    }
                    if (method.equals("SVM")) {
                        output = SVM.test(output, model);
                    }
                    String annot_nosr = dir + "/" + "fold" + n + "/" + (new File(output)).getName();

                    (new File(output)).renameTo(new File(annot_nosr));
                    /*PipesFile nlpannot_nosr = new PipesFile();
                    nlpannot_nosr.loadFile(new File(annot_nosr));
                    ((PipesFile) nlpannot_nosr).isWellFormedOptimist();*/




                    // Build key
                    key = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test" + n + "/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test" + n + "/" + elem + "-extents.tab", elem);
                    key =  TempEvalFiles.merge_attribs(localDatasetPath + lang + "/test" + n + "/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/test" + n + "/" + elem + "-attributes.tab", elem);
                    key = Classification.get_classik(key, lang);


                    System.out.println("Results: " + baseline);
                    Score score2 = scorer.score_class(annot_nosr, key, -1);

                    if (show == null || show.equalsIgnoreCase("summary")) {
                        score2.print("");
                    } else {
                        if (show.equalsIgnoreCase("detail")) {
                            score2.print("detail");
                        }
                    }

                    baseline_scores.add((Score) score2.clone());


                    // EN QUE MEJORAN LOS ROLES?
                    //scorer.compare_scores(score, score2);


                }
            }
            }


            // Aquí puedo tener 10 pares de scores y calcular si la differencia es significativa
            String element = "class";

            double mean_F1 = 0.0;

            if (approach_scores.size() == 10) {
                for (int fold = 0; fold < 10; fold++) {
                    mean_F1 += approach_scores.get(fold).getF1TokenLevel(element);
                }
                mean_F1 = mean_F1 / 10.0;
                System.out.println("\n\n\n----------------10 Fold (approach: " + approach + ")\n--> comp\\_accuracy\n \t& " + StringUtils.twoDecPosS(mean_F1));
            }

            if (baseline_scores.size() == 10 && approach_scores.size() == 10) {
                double mean_F1b = 0.0;
                double mean_F1diff = 0.0;
                double mean_F1imp = 0.0;
                double mean_F1err = 0.0;
                double diffsF1[] = new double[10];
                double impF1[] = new double[10];
                double errF1[] = new double[10];
                for (int fold = 0; fold < 10; fold++) {
                    diffsF1[fold] = approach_scores.get(fold).getF1TokenLevel(element) - baseline_scores.get(fold).getF1TokenLevel(element);

                    impF1[fold] = ((approach_scores.get(fold).getF1TokenLevel(element) * 100.0) / baseline_scores.get(fold).getF1TokenLevel(element)) - 100.0;

                    errF1[fold] = 100 * (((1 - baseline_scores.get(fold).getF1TokenLevel(element)) - (1 - approach_scores.get(fold).getF1TokenLevel(element))) / (1 - baseline_scores.get(fold).getF1TokenLevel(element)));

                    mean_F1b += baseline_scores.get(fold).getF1TokenLevel(element);

                    mean_F1diff += diffsF1[fold];

                    mean_F1imp += impF1[fold];

                    mean_F1err += errF1[fold];
                }
                mean_F1b = mean_F1b / 10.0;

                mean_F1diff = mean_F1diff / 10.0;

                mean_F1imp = mean_F1imp / 10.0;

                mean_F1err = mean_F1err / 10.0;

                System.out.println("\n----------------10 Fold (baseline: " + baseline + ")\n--> comp\\_accuracy\n \t& " + StringUtils.twoDecPosS(mean_F1b));


                System.out.println("\n----------------10 Fold DIFFERENCES\n-->       c acc");
                System.out.println("--> DIFF:  \t& " + StringUtils.twoDecPosS(mean_F1diff));
                System.out.println("--> IMP:   \t& " + StringUtils.twoDecPosS(mean_F1imp));
                System.out.println("--> ERR: \t& " + StringUtils.twoDecPosS(mean_F1err));


                System.out.println("\n\ndiffACC significance:\n " +T_test.paired_t_test(diffsF1) + "");

                System.out.println("\n\nimpACC significance:\n " +T_test.paired_t_test(impF1) + "");

                System.out.println("\n\nerrACC significance:\n " +T_test.paired_t_test(errF1) + "");


                boolean latex = true;

                if (latex) {
                    System.out.println("\n\n\\begin{table} [h]\n\\begin{footnotesize}\n\\begin{center}\n\\begin{tabular} {lccccc}\n\\hline\\rule{-2pt}{8pt}\n& \\textbf{" + baseline + "} & \\hspace{0.2cm} & \\textbf{" + approach + "}  & \\hspace{0.2cm} & \\textbf{Difference}\\\\");
                    System.out.println("\\textbf{Dataset \\hspace{0.2cm}} \t& \\textbf{accuracy} \t& \t& \\textbf{accuracy} \t& \t& \\textbf{accuracy}\\\\\n  \\hline\\rule{-2pt}{8pt}");
                    for (int fold = 0; fold < 10; fold++) {
                        System.out.println("\\textbf{Fold-" + (fold+1) + "} \t&  " + StringUtils.twoDecPosS(baseline_scores.get(fold).getF1TokenLevel(element)) + " \t& \t&  " + StringUtils.twoDecPosS(approach_scores.get(fold).getF1TokenLevel(element)) + " \t& \t&  " + StringUtils.twoDecPosS(diffsF1[fold]) + " \\\\");
                    }
                    System.out.println("\\hline\\rule{-2pt}{8pt}");
                    System.out.println("\\textbf{Mean} \t&  " + StringUtils.twoDecPosS(mean_F1b) + " \t& \t&  " + StringUtils.twoDecPosS(mean_F1) + " \t& \t& " + StringUtils.twoDecPosS(mean_F1diff) + " \\\\");
                    System.out.println("\\hline\n\\end{tabular}\n\\ \\\\\\vspace{0.5cm}\\ \\\\");

                    System.out.println("\\begin{tabular} {lrlll}\n\\hline\\rule{-2pt}{8pt} \\textbf{Comparison} 	 & & & & \\\\\n\\textbf{TIPSem/TIPSem-B} & 	& \\textbf{Mean}  	& \\textbf{SD}	& \\textbf{t (p one-tail)} \\\\");

                    System.out.println("\\hline\\rule{-2pt}{8pt}\\textbf{Difference}");
                    String ttest =T_test.paired_t_test(diffsF1);
                    System.out.println("\t& \\textbf{ACC} \t& " + T_test.latex_t_test(ttest));

                    System.out.println("\\hline\\rule{-2pt}{8pt}\\textbf{Improvement \\%}");
                    ttest =T_test.paired_t_test(impF1);
                    System.out.println("\t& \\textbf{ACC} \t& " + T_test.latex_t_test(ttest));

                    System.out.println("\\hline\\rule{-2pt}{8pt}\\textbf{Relative error}");
                    ttest =T_test.paired_t_test(errF1);
                    System.out.println("\\ \\textbf{reduction \\%} \t& \\textbf{ACC} \t& " + T_test.latex_t_test(ttest));
                    System.out.println("\\hline\n\\end{tabular}\n\\end{center}\n\\caption{" + elem + " classification 10-fold (English)}\\label{tab:10-fold-"+elem+"-class}");
                    System.out.println("\\end{footnotesize}\n\\end{table}");
                }
            }



        } catch (Exception e) {
            System.err.println("Errors found (Experimenter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    public static void NormalizationType(String elem, String approach, String lang, String method) {
        NormalizationType(elem, approach, lang, method, null, null);
    }

    public static void NormalizationType(String elem, String approach, String lang, String method, String baseline, String retrain) {
        String output = "", key;
        Scorer scorer = new Scorer();
        try {
            File dir = new File(localDatasetPath + "experiments/" + approach + "/");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Check for features files (train/test)
            if (!(new File(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features")).exists()) {
                BaseTokenFeatures.getFeatures4Tab(lang, localDatasetPath + lang + "/train/base-segmentation.tab", "TempEval2-features", approach);
            }
            if (!(new File(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features")).exists()) {
                BaseTokenFeatures.getFeatures4Tab(lang, localDatasetPath + lang + "/test/entities/base-segmentation.tab", "TempEval2-features", approach);
            }

            String model = dir + "/" + approach + "_timen_" + elem + "_" + lang + "." + method + "model";
            if (!(new File(model)).exists() || retrain != null) {


                output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features", localDatasetPath + lang + "/train/" + elem + "-extents.tab", elem);
                String features = TempEvalFiles.merge_attribs(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/train/" + elem + "-attributes.tab", elem);
                output = Classification.get_classik(features, lang);
                output = TimexNormalization.getTIMEN(features, output, lang);

                if (method.equals("CRF")) {
                    output = CRF.train(output, approach + "_timen_" + elem + ".template");
                }
                if (method.equals("SVM")) {
                    output = SVM.train(output, approach + "_timen_" + elem + ".template");
                }
                (new File(output)).renameTo(new File(model));
                //(new File(output)).renameTo(new File((new File(output)).getCanonicalPath().substring((new File(output)).getName().indexOf(approach))));
            }

            String features = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test/entities/" + elem + "-extents.tab", elem);
            output = Classification.get_classik(features, lang);
            output = TimexNormalization.getTIMEN(features, output, lang);

            if (method.equals("CRF")) {
                output = CRF.test(output, model);
            }
            if (method.equals("SVM")) {
                output = SVM.test(output, model);
            }

            String annot = dir + "/" + (new File(output)).getName();
            (new File(output)).renameTo(new File(annot));

            key = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test/entities/" + elem + "-extents.tab", elem);
            String keyfeatures = TempEvalFiles.merge_attribs(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/test/entities/" + elem + "-attributes.tab", elem);
            key = Classification.get_classik(keyfeatures, lang);
            key = TimexNormalization.getTIMEN(keyfeatures, key, lang);



            // TempEvalFiles-2 results
            System.out.println("Results: " + approach);
            //TempEval_scorer.score_entities(extents, TempEvalpath +lang+"/test/entities/"+ elem + "-attributes.tab", lang, elem);

            // AnnotScore results
            Score score = scorer.score_class(annot, key, -1);
            score.print("");


            if (baseline != null) {
                File baselinef = null;
                if (method.equals("CRF")) {
                    baselinef = new File(CRF.program_path + baseline + "_timen_" + elem + ".template");
                }
                if (method.equals("SVM")) {
                    baselinef = new File(SVM.program_path + baseline + "_timen_" + elem + ".template");
                }


                if (baselinef.exists()) {
                    model = dir + "/" + baseline + "_timen_" + elem + "_" + lang + "." + method + "model";
                    if (!(new File(model)).exists() || retrain != null) {
                        output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features", localDatasetPath + lang + "/train/" + elem + "-extents.tab", elem);
                        features = TempEvalFiles.merge_attribs(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/train/" + elem + "-attributes.tab", elem);
                        output = Classification.get_classik(features, lang);
                        output = TimexNormalization.getTIMEN(features, output, lang);

                        if (method.equals("CRF")) {
                            output = CRF.train(output, baseline + "_timen_" + elem + ".template");
                        }
                        if (method.equals("SVM")) {
                            output = SVM.train(output, baseline + "_timen_" + elem + ".template");
                        }

                        (new File(output)).renameTo(new File(model));
                    }


                    features = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test/entities/" + elem + "-extents.tab", elem);
                    output = Classification.get_classik(features, lang);
                    output = TimexNormalization.getTIMEN(features, output, lang);


                    if (method.equals("CRF")) {
                        output = CRF.test(output, model);
                    }
                    if (method.equals("SVM")) {
                        output = SVM.test(output, model);
                    }
                    String annot_nosr = dir + "/" + (new File(output)).getName();

                    (new File(output)).renameTo(new File(annot_nosr));

                    // Build key
                    key = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test/entities/" + elem + "-extents.tab", elem);
                    keyfeatures = TempEvalFiles.merge_attribs(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/test/entities/" + elem + "-attributes.tab", elem);
                    key = Classification.get_classik(keyfeatures, lang);
                    key = TimexNormalization.getTIMEN(keyfeatures, key, lang);


                    System.out.println("Results: " + baseline);
                    Score score2 = scorer.score_class(annot_nosr, key, -1);
                    score2.print("");

                    scorer.compare_scores(score, score2);

                }
            }


        } catch (Exception e) {
            System.err.println("Errors found (Experimenter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    public static void Normalization(String elem, String approach, String lang, String baseline) {
        String output = "", key;
        Scorer scorer = new Scorer();
        try {
            File dir = new File(localDatasetPath + "experiments/" + approach + "/");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Check for features files (train/test)
            if (!(new File(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features")).exists()) {
                BaseTokenFeatures.getFeatures4Tab(lang, localDatasetPath + lang + "/train/base-segmentation.tab", "TempEval2-features", approach);
            }
            if (!(new File(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features")).exists()) {
                BaseTokenFeatures.getFeatures4Tab(lang, localDatasetPath + lang + "/test/entities/base-segmentation.tab", "TempEval2-features", approach);
            }

            output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features", localDatasetPath + lang + "/train/" + elem + "-extents.tab", elem);
            String features = TempEvalFiles.merge_attribs(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/train/" + elem + "-attributes.tab", elem);
            output = Classification.get_classik(features, lang);
            output = TimexNormalization.getTIMEN(features, output, lang);
            if (baseline == null) {
                output = TimexNormalization.get_normalized_values(output, lang);
            } else {
                output = TimexNormalization.get_normalized_values_baseline(output);
            }

            System.out.println(output);
            String annot = dir + "/" + (new File(output)).getName();
            (new File(output)).renameTo(new File(annot));

            key = TempEvalFiles.merge_extents(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features", localDatasetPath + lang + "/train/" + elem + "-extents.tab", elem);
            String keyfeatures = TempEvalFiles.merge_attribs(localDatasetPath + lang + "/train/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/train/" + elem + "-attributes.tab", elem);
            key = Classification.get_classik(keyfeatures, lang);
            key = TimexNormalization.getTIMEN(keyfeatures, key, lang);
            key = TimexNormalization.get_key_normalized_values(key);


            // TempEvalFiles-2 results
            System.out.println("Trainset Results: " + approach);
            //TempEval_scorer.score_entities(extents, TempEvalpath +lang+"/test/entities/"+ elem + "-attributes.tab", lang, elem);

            // AnnotScore results
            Score score = scorer.score_class(annot, key, -1);
            score.print("detail");


            output = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test/entities/" + elem + "-extents.tab", elem);
            features = TempEvalFiles.merge_attribs(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/test/entities/" + elem + "-attributes.tab", elem);
            output = Classification.get_classik(features, lang);
            output = TimexNormalization.getTIMEN(features, output, lang);
            if (baseline == null) {
                output = TimexNormalization.get_normalized_values(output, lang);
            } else {
                output = TimexNormalization.get_normalized_values_baseline(output);
            }

            System.out.println(output);
            annot = dir + "/" + (new File(output)).getName();
            (new File(output)).renameTo(new File(annot));

            key = TempEvalFiles.merge_extents(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features", localDatasetPath + lang + "/test/entities/" + elem + "-extents.tab", elem);
            keyfeatures = TempEvalFiles.merge_attribs(localDatasetPath + lang + "/test/entities/base-segmentation.TempEval2-features-annotationKey-" + elem, localDatasetPath + lang + "/test/entities/" + elem + "-attributes.tab", elem);
            key = Classification.get_classik(keyfeatures, lang);
            key = TimexNormalization.getTIMEN(keyfeatures, key, lang);
            key = TimexNormalization.get_key_normalized_values(key);


            // TempEvalFiles-2 results
            System.out.println("Testset Results: " + approach);
            //TempEval_scorer.score_entities(extents, TempEvalpath +lang+"/test/entities/"+ elem + "-attributes.tab", lang, elem);

            // AnnotScore results
            score = scorer.score_class(annot, key, -1);
            score.print("detail");

        } catch (Exception e) {
            System.err.println("Errors found (Experimenter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }


}
