package com.cognitionis.tipsem;

import com.cognitionis.feature_builder.BaseTokenFeatures;
import com.cognitionis.utils_basickit.FileUtils;
import com.cognitionis.utils_basickit.StringUtils;
import com.cognitionis.nlp_files.NLPFile;
import com.cognitionis.nlp_files.XMLFile;
import com.cognitionis.nlp_files.TempEvalFiles;
import com.cognitionis.nlp_files.PipesFile;
import com.cognitionis.nlp_files.PlainFile;
import com.cognitionis.timeml_basickit.Link;
import com.cognitionis.timeml_basickit.Timex;
import com.cognitionis.timeml_basickit.Event;
import com.cognitionis.timeml_basickit.comparators.AscINT_eiid_Comparator;
import com.cognitionis.timeml_basickit.comparators.AscINT_lid_Comparator;
import java.io.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Hector Llorens
 * @since 2011
 */
public class FileConverter {

    public static String pipes2tml(PipesFile pipesfile, HashMap<String, Timex> DCTs, HashMap<String, HashMap<String, Event>> makeinstances, HashMap<String, HashMap<String, Link>> linksHash) {
        return pipes2tml(pipesfile, DCTs, makeinstances, linksHash, null, null,"");
    }

    public static String pipes2tml(PipesFile pipesfile, HashMap<String, Timex> DCTs, HashMap<String, HashMap<String, Event>> makeinstances, HashMap<String, HashMap<String, Link>> linksHash, String header, String footer, String last_text_blanks) {
        String outputfile = null;
        BufferedWriter outfile = null;
        int linen = 0;
        int fake_id =1;

        try {
            int iob2col = pipesfile.getColumn("element\\(IOB2\\)");
            int attrscol = iob2col + 1;
            int wordcol = pipesfile.getColumn("word");
            BufferedReader pipesreader = new BufferedReader(new FileReader(pipesfile.getFile()));
            String inElem = "";

            try {
                String pipesline;
                String prev_token = "";
                String[] pipesarr = null;
                String curr_fileid = "";
                String curr_sentN = "";

                while ((pipesline = pipesreader.readLine()) != null) {
                    linen++;
                    pipesarr = pipesline.split("\\|");

                    if (!curr_fileid.equals(pipesarr[0])) {
                        if (outfile != null && !curr_fileid.equals("")) {
                            outfile.write(last_text_blanks+"</TEXT>\n\n");

                            if (makeinstances != null && makeinstances.containsKey(curr_fileid)) {
                                outfile.write("\n");
                                HashMap<String, Event> mk = makeinstances.get(curr_fileid);
                                List<String> sortedKeys = new ArrayList();
                                sortedKeys.addAll(mk.keySet());
                                Collections.sort(sortedKeys, new AscINT_eiid_Comparator());
                                for (int i = 0; i < sortedKeys.size(); i++) {
                                    String eiid = sortedKeys.get(i);
                                    outfile.write("<MAKEINSTANCE eiid=\"" + eiid + "\"  eventID=\"" + mk.get(eiid).get_id() + "\" ");
                                    if (mk.get(eiid).get_POS() != null) {
                                        outfile.write("pos=\"" + mk.get(eiid).get_POS() + "\" ");
                                    }
                                    if (mk.get(eiid).get_tense() != null) {
                                        // TODO: Here is the place to make a hack for Spanish to convert VMN0 to tense NONE
                                        outfile.write("tense=\"" + mk.get(eiid).get_tense() + "\" ");
                                    }
                                    if (mk.get(eiid).get_aspect() != null) {
                                        outfile.write("aspect=\"" + mk.get(eiid).get_aspect() + "\" ");
                                    }
                                    if (mk.get(eiid).get_polarity() != null) {
                                        outfile.write("polarity=\"" + mk.get(eiid).get_polarity() + "\" ");
                                    }
                                    if (mk.get(eiid).get_modality() != null) {
                                        outfile.write("modality=\"" + mk.get(eiid).get_modality() + "\" ");
                                    }
                                    outfile.write("/>\n");
                                }
                            }

                            if (linksHash != null && linksHash.containsKey(curr_fileid)) {
                                outfile.write("\n");
                                HashMap<String, Link> links = linksHash.get(curr_fileid);
                                List<String> sortedKeys = new ArrayList();
                                sortedKeys.addAll(links.keySet());
                                Collections.sort(sortedKeys, new AscINT_lid_Comparator());
                                for (int i = 0; i < sortedKeys.size(); i++) {
                                    Link l = links.get(sortedKeys.get(i));
                                    if (l.get_type().startsWith("tlink-event-timex") || l.get_type().equalsIgnoreCase("tlink-event-duration")) {
                                        outfile.write("<TLINK lid=\"" + l.get_id() + "\" relType=\"" + l.get_category().toUpperCase() + "\" eventInstanceID=\"" + l.get_id1() + "\" relatedToTime=\"" + l.get_id2() + "\" />\n"); //<!-- link-type=\"" + l.get_type() + "\" -->\n");
                                    }
                                    if (l.get_type().startsWith("tlink-main-event") || l.get_type().equalsIgnoreCase("tlink-sub-event")) {
                                        outfile.write("<TLINK lid=\"" + l.get_id() + "\" relType=\"" + l.get_category().toUpperCase() + "\" eventInstanceID=\"" + l.get_id1() + "\" relatedToEventInstance=\"" + l.get_id2() + "\" />\n"); //<!-- link-type=\"" + l.get_type() + "\" -->\n");
                                    }
                                    /*if (l.get_type().equalsIgnoreCase("aspectual")) {
                                    outfile.write("<ALINK lid=\"" + l.get_id() + "\" relType=\"" + l.get_category().toUpperCase() + "\" leid=\"" + l.get_id1() + "\" leid2=\"" + l.get_id2() + "\" />\n");
                                    }*/
                                }
                            }

                            if (footer == null) {
                                outfile.write("\n</TimeML>\n");
                            } else {
                                outfile.write(footer);
                            }

                            outfile.close();
                            outfile = null;
                            fake_id=1;
                        }

                        outputfile = pipesfile.getFile().getParent() + File.separator + pipesarr[0] + ".tml";
                        outfile = new BufferedWriter(new FileWriter(outputfile));


                        curr_fileid = pipesarr[0]; // initialize file
                        if (header == null || header.trim().equals("")) {
                            outfile.write("<?xml version=\"1.0\" ?>");
                            outfile.write("\n<TimeML xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://timeml.org/timeMLdocs/TimeML_1.2.1.xsd\">\n");
                            outfile.write("\n<DOCID>" + curr_fileid + "</DOCID>\n");
                            if (DCTs != null && DCTs.containsKey(pipesarr[0])) {
                                outfile.write("\n<DCT><TIMEX3 tid=\"" + DCTs.get(pipesarr[0]).get_id() + "\" type=\"DATE\" value=\"" + DCTs.get(pipesarr[0]).get_value() + "\" temporalFunction=\"false\" functionInDocument=\"CREATION_TIME\">" + DCTs.get(pipesarr[0]).get_value() + "</TIMEX3></DCT>\n");
                            }
                        } else {
                            outfile.write(header);
                        }
                        outfile.write("<TEXT>\n");

                    }
                    if (!curr_sentN.equals(pipesarr[1])) {
                        curr_sentN = pipesarr[1];
                        //outfile.write("\n");
                    }
                    String preceding_blanks = "";
                    if (pipesarr[2].matches(".*-[stn]*")) {
                        String[] blanksarr = pipesarr[2].split("-");
                        if (blanksarr.length > 1) {
                            int x = blanksarr[1].length();
                            for (int i = 0; i < x; i++) {
                                if (blanksarr[1].charAt(i) == 's') {
                                    preceding_blanks += " ";
                                } else {
                                    if (blanksarr[1].charAt(i) == 'n') {
                                        preceding_blanks += "\n";
                                    } else {
                                        if (blanksarr[1].charAt(i) == 't') {
                                            preceding_blanks += "\t";
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // the old approach
                        if (pipesarr[2].matches(".*-[0-9]+")) {
                            String[] blanksarr = pipesarr[2].split("-");
                            int x = Integer.parseInt(blanksarr[1]);
                            for (int i = 0; i < x; i++) {
                                preceding_blanks += " ";
                            }
                            if (blanksarr.length > 2) {
                                x = Integer.parseInt(blanksarr[2]);
                                for (int i = 0; i < x; i++) {
                                    preceding_blanks += "\n";
                                }
                            } else {
                                if (blanksarr[0].equals("0")) {
                                    preceding_blanks += "\n";
                                }
                            }
                            if (blanksarr.length > 3) {
                                x = Integer.parseInt(blanksarr[3]);
                                for (int i = 0; i < x; i++) {
                                    preceding_blanks += "\t";
                                }
                            }
                        } else {
                            if ((pipesfile.getLanguage().equalsIgnoreCase("en") && !pipesarr[2].equals("0"))
                                 ||(pipesfile.getLanguage().equalsIgnoreCase("es") && !pipesarr[2].equals("1"))
                                 ) {
                                if (!pipesarr[3].matches("(\\.|,|;|:|\\?|!|\\))") || prev_token.matches("[(¿¡]")){
                                    if((pipesfile.getLanguage().equalsIgnoreCase("en") && !(pipesarr[3].equals("n") && prev_token.matches("(do(es)?|have|has|had|is|are|was|were|ca|could|wo|did|would|we|they|he|she|you)"))
                                        && !(pipesarr[3].matches("'(t|s|ll|d|re|m|ve)")))
                                       ||
                                       (pipesfile.getLanguage().equalsIgnoreCase("es")) // extend if necessary
                                      ) {
                                    preceding_blanks = " ";
                                    }
                                }
                            } else {
                                if ((pipesfile.getLanguage().equalsIgnoreCase("en") && !pipesarr[1].equals("0"))
                                    ||(pipesfile.getLanguage().equalsIgnoreCase("es") && !pipesarr[1].equals("1"))
                                        ) {
                                    preceding_blanks = "\n";
                                }
                            }
                        }
                    }

                    if (pipesarr[iob2col].startsWith("B")) {
                        if (!inElem.equals("")) {
                            outfile.write("</" + inElem + ">");
                            inElem = "";
                        }
                        inElem = pipesarr[iob2col].substring(2).toUpperCase();
                        outfile.write(preceding_blanks + "<" + inElem);
                        if(pipesarr.length>attrscol){
                            String[] attrsarr = pipesarr[attrscol].trim().split(";");
                            for (int i = 0; i < attrsarr.length; i++) {
                                if (attrsarr[i].indexOf('=') > 0) {
                                    String attrname = attrsarr[i].substring(0, attrsarr[i].indexOf('='));
                                    String attrvalue = attrsarr[i].substring(attrsarr[i].indexOf("=") + 1);
                                    if (attrvalue.matches("\".*\"")) {
                                        attrvalue = attrvalue.substring(1, attrvalue.length() - 1);
                                    }
                                    if (attrname.matches("(type|value|class|tense|aspect|val|eid|tid|sid)")) {
                                        outfile.write(" " + attrname + "=\"" + attrvalue + "\"");
                                    }
                                }
                            }
                        }else{
                            // fake attribs
                            if(inElem.matches("(?i)(TIMEX|TIMEX3)")){
                                 outfile.write(" tid=\""+fake_id+"\" type=\"DATE\" value=\"XXXX-XX-XX\" ");
                            }
                            if(inElem.matches("(?i)EVENT")){
                                outfile.write(" eid=\""+fake_id+"\" class=\"OCCURRENCE\" ");
                            }
                            fake_id++;
                        }
                        outfile.write(">");
                    }
                    if (pipesarr[iob2col].startsWith("I")) {
                        if (inElem.equals("")) {
                            throw new Exception("Found I-X element without B-X ("+pipesarr[iob2col]+")");
                        }
                    }
                    if (pipesarr[iob2col].equals("O")) {
                        if (!inElem.equals("")) {
                            outfile.write("</" + inElem + ">");
                            inElem = "";
                        }
                    }
                    if (!pipesarr[iob2col].startsWith("B")) {
                        outfile.write(preceding_blanks);
                    }
                    outfile.write(pipesarr[wordcol].replaceAll("&#124;", "|").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;"));

                    prev_token = pipesarr[wordcol];
                }

                if (!inElem.equals("")) {
                    outfile.write("</" + inElem + ">");
                    inElem = "";
                }

                outfile.write(last_text_blanks+"</TEXT>\n\n");
                if (makeinstances != null && makeinstances.containsKey(pipesarr[0])) {
                    outfile.write("\n");
                    HashMap<String, Event> mk = makeinstances.get(pipesarr[0]);
                    List<String> sortedKeys = new ArrayList();
                    sortedKeys.addAll(mk.keySet());
                    Collections.sort(sortedKeys, new AscINT_eiid_Comparator());
                    for (int i = 0; i < sortedKeys.size(); i++) {
                        String eiid = sortedKeys.get(i);
                        outfile.write("<MAKEINSTANCE eiid=\"" + eiid + "\"  eventID=\"" + mk.get(eiid).get_id() + "\" ");
                        if (mk.get(eiid).get_POS() != null) {
                            outfile.write("pos=\"" + mk.get(eiid).get_POS() + "\" ");
                        }
                        if (mk.get(eiid).get_tense() != null) {
                            outfile.write("tense=\"" + mk.get(eiid).get_tense() + "\" ");
                        }
                        if (mk.get(eiid).get_aspect() != null) {
                            outfile.write("aspect=\"" + mk.get(eiid).get_aspect() + "\" ");
                        }
                        if (mk.get(eiid).get_polarity() != null) {
                            outfile.write("polarity=\"" + mk.get(eiid).get_polarity() + "\" ");
                        }
                        if (mk.get(eiid).get_modality() != null) {
                            outfile.write("modality=\"" + mk.get(eiid).get_modality() + "\" ");
                        }
                        outfile.write("/>\n");
                    }
                }

                if (linksHash != null && linksHash.containsKey(pipesarr[0])) {
                    outfile.write("\n");
                    HashMap<String, Link> links = linksHash.get(pipesarr[0]);
                    List<String> sortedKeys = new ArrayList();
                    sortedKeys.addAll(links.keySet());
                    Collections.sort(sortedKeys, new AscINT_lid_Comparator());
                    for (int i = 0; i < sortedKeys.size(); i++) {
                        Link l = links.get(sortedKeys.get(i));
                        if (l.get_type().startsWith("tlink-event-timex") || l.get_type().equalsIgnoreCase("tlink-event-duration")) {
                            outfile.write("<TLINK lid=\"" + l.get_id() + "\" relType=\"" + l.get_category().toUpperCase() + "\" eventInstanceID=\"" + l.get_id1() + "\" relatedToTime=\"" + l.get_id2() + "\" />\n"); //<!-- link-type=\"" + l.get_type() + "\" -->\n");
                        }
                        if (l.get_type().startsWith("tlink-main-event") || l.get_type().equalsIgnoreCase("tlink-sub-event")) {
                            outfile.write("<TLINK lid=\"" + l.get_id() + "\" relType=\"" + l.get_category().toUpperCase() + "\" eventInstanceID=\"" + l.get_id1() + "\" relatedToEventInstance=\"" + l.get_id2() + "\" />\n"); //<!-- link-type=\"" + l.get_type() + "\" -->\n");
                        }
                        /*if (l.get_type().equalsIgnoreCase("aspectual")) {
                        outfile.write("<ALINK lid=\"" + l.get_id() + "\" relType=\"" + l.get_category().toUpperCase() + "\" leid=\"" + l.get_id1() + "\" leid2=\"" + l.get_id2() + "\" />\n");
                        }*/
                    }
                }

                if (footer == null || footer.trim().equals("")) {
                    outfile.write("\n</TimeML>\n");
                } else {
                    outfile.write(footer);
                }

            } finally {
                if (pipesreader != null) {
                    pipesreader.close();
                }
                if (outfile != null) {
                    outfile.close();
                }
            }



        } catch (Exception e) {
            System.err.println("Errors found (TempEval):\n\t" + e.toString() + " (Reading line " + linen + ")\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
            return null;
        }
        return outputfile;
    }

    public static void tmlfile2features4training(XMLFile tmlfile, String pipefeaturesFile) { // add parameter, temporal aware, limited choice
        try {
            // tmlfile is already ensured to be tml

            // get paired-roth tokens
            PipesFile nlpfile = new PipesFile(pipefeaturesFile);
            ((PipesFile) nlpfile).isWellFormedOptimist();
            nlpfile.setLanguage(tmlfile.getLanguage());

            // get base-segmentation
            BufferedWriter outfile = new BufferedWriter(new FileWriter(new File(tmlfile.getFile().getParent() +File.separator +"base-segmentation.tab")));
            BufferedReader reader = new BufferedReader(new FileReader(new File(pipefeaturesFile)));

            try {
                String pipesline;
                String[] pipesarr = null;

                while ((pipesline = reader.readLine()) != null) {
                    // esto es innecesario y se podría hacer TODO AQUÍ
                    pipesarr = pipesline.split("\\|");
                    outfile.write(pipesarr[0] + "\t" + pipesarr[1] + "\t" + pipesarr[2] + "\t" + pipesarr[3] + "\n");
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
                if (outfile != null) {
                    outfile.close();
                }
            }

            // merge xml_tok everything
            String timexpipes = ((PipesFile) nlpfile).merge_tok_n_xml(tmlfile.getFile().getCanonicalPath(), "TEXT", "timex3", ".*", null);
            String eventpipes = ((PipesFile) nlpfile).merge_tok_n_xml(tmlfile.getFile().getCanonicalPath(), "TEXT", "event", ".*", null);
            //String signalpipes = ((PipesFile) nlpfile).merge_tok_n_xml(tmlfile.getFile().getCanonicalPath(), "TEXT", "signal", ".*", null);

            // get extents tabs (timex,signal,event) [if present]
            nlpfile=new PipesFile(timexpipes);
            ((PipesFile) nlpfile).isWellFormedOptimist();
            /*String tab = TempEvalFiles.generate_tab_extents(nlpfile);
            tab = FileUtils.renameTo(tab, "\\.TempEval2-features-annotationKey-timex3-extents\\.tab", "\\.timex-extents");
            tab = TempEvalFiles.generate_tab_attribs(nlpfile);
            tab = FileUtils.renameTo(tab, "\\.TempEval2-features-annotationKey-timex3-attributes\\.tab", "\\.timex-attribs");*/
            if (!TempEvalFiles.generate_tab_extents_and_attribs_with_real_id(nlpfile, "timex", "tid")) {
                throw new Exception("Error generating extents and attribs files with real ids.");
            }
            //(new File(timexpipes)).delete();


            nlpfile=new PipesFile(eventpipes);
            ((PipesFile) nlpfile).isWellFormedOptimist();
            /*tab = TempEvalFiles.generate_tab_extents(nlpfile);
            tab = FileUtils.renameTo(tab, "\\.TempEval2-features-annotationKey-event-extents\\.tab", "\\.event-extents");
            tab = TempEvalFiles.generate_tab_attribs(nlpfile);
            tab = FileUtils.renameTo(tab, "\\.TempEval2-features-annotationKey-event-attributes\\.tab", "\\.event-attribs");*/
            if (!TempEvalFiles.generate_tab_extents_and_attribs_with_real_id(nlpfile, "event", "eid")) {
                throw new Exception("Error generating extents and attribs files with real ids.");
            }
            //(new File(eventpipes)).delete();

            /*nlpfile.loadFile(new File(signalpipes));
            ((PipesFile) nlpfile).isWellFormedOptimist();
            tab = generate_tab_extents(nlpfile);
            tab = FileUtils.renameTo(tab, "\\.TempEval2-features-annotationKey-signal-extents\\.tab", "\\.signal-extents");
            (new File(signalpipes)).delete();
             */

            // get dcts (easy)
            if (!((new File(tmlfile.getFile().getParent() + "/dct.tab")).exists())) {
                TempEvalFiles.tml2dct_tab(tmlfile.getFile().getCanonicalPath());
            }

            // TODO get tlinks tab
            //tmlfile.getFile().getCanonicalPath()

            String timex_merged = TempEvalFiles.merge_extents(pipefeaturesFile, tmlfile.getFile().getParent() + File.separator + "timex-extents.tab", "timex");
            timex_merged = TempEvalFiles.merge_attribs(timex_merged, tmlfile.getFile().getParent() + File.separator + "timex-attributes.tab", "timex");
            String event_merged = TempEvalFiles.merge_extents(pipefeaturesFile, tmlfile.getFile().getParent() + File.separator + "event-extents.tab", "event");
            event_merged = TempEvalFiles.merge_attribs(event_merged, tmlfile.getFile().getParent() + File.separator + "event-attributes.tab", "event");

            // This can be easier without tags...
            String all_merged = PipesFile.merge_pipes(timex_merged, event_merged);

            PipesFile features_annotated = new PipesFile(all_merged);
            features_annotated.isWellFormedOptimist();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(tmlfile.getFile());
            doc.getDocumentElement().normalize();
            Timex dct_timex = null;

            Element dct = null;
            if(doc.getElementsByTagName("DCT").getLength()>0){
                dct=((Element) ((NodeList) ((Element) doc.getElementsByTagName("DCT").item(0)).getElementsByTagName("TIMEX3")).item(0));
            }
            if (dct != null) {
                dct_timex = new Timex(dct.getAttribute("tid"), dct.getTextContent(), dct.getAttribute("type"), dct.getAttribute("value"), tmlfile.getFile().getName(), -1, -1, true);
            }

            //load everything and make sure it is properly linked

            HashMap<String, Timex> timexes = new HashMap<String, Timex>();
            HashMap<String, Event> makeinstances = new HashMap<String, Event>();
            HashMap<String, ArrayList<String>> event_mk_index = new HashMap<String, ArrayList<String>>();

            String current_tag = "MAKEINSTANCE";
            NodeList current_node = doc.getElementsByTagName(current_tag);
            for (int s = 0; s < current_node.getLength(); s++) {
                Element element = (Element) current_node.item(s);
                if (!event_mk_index.containsKey(element.getAttribute("eventID"))) {
                    ArrayList<String> mks = new ArrayList<String>();
                    mks.add(element.getAttribute("eiid"));
                    event_mk_index.put(element.getAttribute("eventID"), mks);
                } else {
                    event_mk_index.get(element.getAttribute("eventID")).add(element.getAttribute("eiid"));
                }
            }

            ElementFiller.get_features_from_pipes(features_annotated, event_mk_index, timexes, makeinstances);

            String basefile = features_annotated.getFile().getCanonicalPath().substring(0, features_annotated.getFile().getCanonicalPath().indexOf(".TempEval2-features"));
            BufferedWriter e_t_links_features = new BufferedWriter(new FileWriter(new File(basefile + ".e-t-link-features")));
            BufferedWriter e_dct_links_features = new BufferedWriter(new FileWriter(new File(basefile + ".e-dct-link-features")));
            BufferedWriter e_main_links_features = new BufferedWriter(new FileWriter(new File(basefile + ".e-main-link-features")));
            BufferedWriter e_sub_links_features = new BufferedWriter(new FileWriter(new File(basefile + ".e-sub-link-features")));
            BufferedWriter e_t_links_features_key = new BufferedWriter(new FileWriter(new File(basefile + ".e-t-link-features-annotationKey")));
            BufferedWriter e_dct_links_features_key = new BufferedWriter(new FileWriter(new File(basefile + ".e-dct-link-features-annotationKey")));
            BufferedWriter e_main_links_features_key = new BufferedWriter(new FileWriter(new File(basefile + ".e-main-link-features-annotationKey")));
            BufferedWriter e_sub_links_features_key = new BufferedWriter(new FileWriter(new File(basefile + ".e-sub-link-features-annotationKey")));
            String docid = tmlfile.getFile().getName() + ".plain";

            try {
                current_tag = "TLINK";
                current_node = doc.getElementsByTagName(current_tag);
                for (int s = 0; s < current_node.getLength(); s++) {
                    Element element = (Element) current_node.item(s);
                    String lid = element.getAttribute("lid");
                    String relType = element.getAttribute("relType");
                    if (relType.matches("(DURING|DURING_INV|IDENTITY)")) {
                        relType = "SIMULTANEOUS";
                    }
                    String mkinstance1 = null;
                    String mkinstance2 = null;
                    String timex1 = null;
                    String timex2 = null;

                    try{
                    // event-event
                    if (element.hasAttribute("eventInstanceID") && element.hasAttribute("relatedToEventInstance")) {
                        mkinstance1 = element.getAttribute("eventInstanceID");
                        mkinstance2 = element.getAttribute("relatedToEventInstance");
                        Event e1 = makeinstances.get(mkinstance1);
                        Event e2 = makeinstances.get(mkinstance2);
                        // Order by textual order (for normalization)
                        if (e1.get_sent_num() > e2.get_sent_num() || (e1.get_sent_num() == e2.get_sent_num() && e1.get_tok_num() > e2.get_tok_num())) {
                            e1 = e2;
                            e2 = makeinstances.get(mkinstance1);
                            relType = Link.reverseRelationCategory(relType);
                        }

                        // guess type
                        if (e1.get_sent_num() != e2.get_sent_num()) {
                            // event main (inter-sentential) relation
                            // file|lid|eiid1|eiid2|e1_class|e1_pos|e1_token|e1_tense-aspect|e2_class|e2_pos|e2_token|e2_tense-aspect
                            e_main_links_features.write(docid + "|" + lid + "|" + e1.get_eiid() + "|" + e2.get_eiid() + "|" + e1.get_class() + "|" + e1.get_POS() + "|" + e1.get_expression() + "|" + e1.get_tense() + "|" + e1.get_tense() + "-" + e1.get_aspect() + "|" + e2.get_class() + "|" + e2.get_POS() + "|" + e2.get_expression() + "|" + e2.get_tense() + "|" + e2.get_tense() + "-" + e2.get_aspect() + "\n");
                            e_main_links_features_key.write(docid + "|" + lid + "|" + e1.get_eiid() + "|" + e2.get_eiid() + "|" + e1.get_class() + "|" + e1.get_POS() + "|" + e1.get_expression() + "|" + e1.get_tense() + "|" + e1.get_tense() + "-" + e1.get_aspect() + "|" + e2.get_class() + "|" + e2.get_POS() + "|" + e2.get_expression() + "|" + e2.get_tense() + "|" + e2.get_tense() + "-" + e2.get_aspect() + "|" + relType + "\n");
                        } else {
                            // event sub (intra-sentential) relation
                            String syntrelation = "none";
                            if (e1.get_phra_id().equals(e2.get_phra_id())) {
                                syntrelation = "equal";
                            }
                            if (e1.get_syntLevel() < e2.get_syntLevel()) {
                                syntrelation = ">"; // inverse because the lower synt the more governing
                            } else {
                                if (e1.get_syntLevel() > e2.get_syntLevel()) {
                                    syntrelation = "<"; // inverse because the lower synt the more governing
                                }
                            }
                            //file|lid|eiid1|eiid2|e1_class|e1_pos|e1_token|e1_tense|e1_tense-aspect|e1_govPP|e1_govTMPSub|e2_class|e2_pos|e2_token|e2_tense|e2_tense-aspect|e2_govPP|e2_govTMPSub|syntrel
                            e_sub_links_features.write(docid + "|" + lid + "|" + e1.get_eiid() + "|" + e2.get_eiid() + "|" + e1.get_class() + "|" + e1.get_POS() + "|" + e1.get_expression() + "|" + e1.get_tense() + "|" + e1.get_tense() + "-" + e1.get_aspect() + "|" + e1.get_govPrep() + "|" + e1.get_govTMPSub() + "|" + e2.get_class() + "|" + e2.get_POS() + "|" + e2.get_expression() + "|" + e2.get_tense() + "|" + e2.get_tense() + "-" + e2.get_aspect() + "|" + e2.get_govPrep() + "|" + e2.get_govTMPSub() + "|" + syntrelation + "\n");
                            e_sub_links_features_key.write(docid + "|" + lid + "|" + e1.get_eiid() + "|" + e2.get_eiid() + "|" + e1.get_class() + "|" + e1.get_POS() + "|" + e1.get_expression() + "|" + e1.get_tense() + "|" + e1.get_tense() + "-" + e1.get_aspect() + "|" + e1.get_govPrep() + "|" + e1.get_govTMPSub() + "|" + e2.get_class() + "|" + e2.get_POS() + "|" + e2.get_expression() + "|" + e2.get_tense() + "|" + e2.get_tense() + "-" + e2.get_aspect() + "|" + e2.get_govPrep() + "|" + e2.get_govTMPSub() + "|" + syntrelation + "|" + relType + "\n");
                        }
                    } // event-time
                    else {
                        // normalize to event-time
                        if (!(element.hasAttribute("timeID") && element.hasAttribute("relatedToTime"))) {
                            if (element.hasAttribute("eventInstanceID") && element.hasAttribute("relatedToTime")) {
                                mkinstance1 = element.getAttribute("eventInstanceID");
                                timex1 = element.getAttribute("relatedToTime");
                            }
                            if (element.hasAttribute("timeID") && element.hasAttribute("relatedToEventInstance")) {
                                mkinstance1 = element.getAttribute("relatedToEventInstance");
                                timex1 = element.getAttribute("timeID");
                                relType = Link.reverseRelationCategory(relType);
                            }

                            Event e1 = makeinstances.get(mkinstance1);
                            Timex t1 = null;
                            if (dct_timex != null && dct_timex.get_id().equals(timex1)) {
                                t1 = dct_timex;
                                
                                // SIMPLIFICATIONS FOR LOW ANNOTATED CATEGORIES
                                if(relType.equals("IBEFORE")){
                                    relType="BEFORE";
                                }
                                if(relType.equals("IAFTER")){
                                    relType="AFTER";
                                }
                                if(relType.matches("(BEGINS|ENDS)")){
                                    relType="IS_INCLUDED";
                                }
                                if(relType.matches("(BEGUN|ENDED)_BY")){
                                    relType="INCLUDES";
                                }

                                //file|lid|eiid|tid|e_class|e_pos|e_token|e_tense-aspect|e_govPP|e_govTMPSub // forget for now |gov_e_class|gov_e_pos|gov_e_token|gov_e_tense-aspect
                                e_dct_links_features.write(docid + "|" + lid + "|" + e1.get_eiid() + "|" + t1.get_id() + "|" + e1.get_class() + "|" + e1.get_POS() + "|" + e1.get_expression() + "|" + e1.get_tense() + "|" + e1.get_tense() + "-" + e1.get_aspect() + "|" + e1.get_govPrep() + "|" + e1.get_govTMPSub() + "|gov_e_class|gov_e_pos|gov_e_token|gov_e_tense-aspect\n");
                                e_dct_links_features_key.write(docid + "|" + lid + "|" + e1.get_eiid() + "|" + t1.get_id() + "|" + e1.get_class() + "|" + e1.get_POS() + "|" + e1.get_expression() + "|" + e1.get_tense() + "|" + e1.get_tense() + "-" + e1.get_aspect() + "|" + e1.get_govPrep() + "|" + e1.get_govTMPSub() + "|gov_e_class|gov_e_pos|gov_e_token|gov_e_tense-aspect|" + relType + "\n");
                            } else {
                                t1 = timexes.get(timex1);
                                String syntrelation = "none";
                                if (e1.get_phra_id().equals(t1.get_phra_id())) {
                                    syntrelation = "samephra";
                                }
                                if (e1.get_subsent_id().equals(t1.get_subsent_id())) {
                                    syntrelation = "samesubsent";
                                } else {
                                    if (e1.get_sent_num() == t1.get_sent_num()) {
                                        syntrelation = "samesent";
                                    }
                                }
                                String tref = t1.get_value();
                                if (t1.isReference()) {
                                    tref = "reference";
                                }
                                if (!t1.get_type().matches("(TIME|DATE)")) {
                                    tref = "duration-set";
                                }

                                // SIMPLIFICATIONS FOR LOW ANNOTATED CATEGORIES
                                if(relType.equals("IBEFORE")){
                                    relType="BEFORE";
                                }
                                if(relType.equals("IAFTER")){
                                    relType="AFTER";
                                }

                                //file|lid|eiid|tid|e_class|e_pos|e_token|e_tense-aspect|e_govPP|e_govTMPSub|t_type|t_ref|t_govPP|t_govTMPSub|synt_relation
                                e_t_links_features.write(docid + "|" + lid + "|" + e1.get_eiid() + "|" + t1.get_id() + "|" + e1.get_class() + "|" + e1.get_POS() + "|" + e1.get_expression() + "|" + e1.get_tense() + "|" + e1.get_tense() + "-" + e1.get_aspect() + "|" + e1.get_govPrep() + "|" + e1.get_govTMPSub() + "|" + t1.get_type() + "|" + tref + "|" + t1.get_govPrep() + "|" + t1.get_govTMPSub() + "|" + syntrelation + "\n");
                                e_t_links_features_key.write(docid + "|" + lid + "|" + e1.get_eiid() + "|" + t1.get_id() + "|" + e1.get_class() + "|" + e1.get_POS() + "|" + e1.get_expression() + "|" + e1.get_tense() + "|" + e1.get_tense() + "-" + e1.get_aspect() + "|" + e1.get_govPrep() + "|" + e1.get_govTMPSub() + "|" + t1.get_type() + "|" + tref + "|" + t1.get_govPrep() + "|" + t1.get_govTMPSub() + "|" + syntrelation + "|" + relType + "\n");
                            }
                        }
                    }


                    } catch (Exception e) {
                        System.err.println("Errors found (TempEval):\n\t" + e.toString() + " lid:"+lid+"\n");
                        if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                            e.printStackTrace(System.err);
                            System.exit(1);
                        }
                    }
                }
            } finally {
                if (e_t_links_features != null) {
                    e_t_links_features.close();
                }
                if (e_dct_links_features != null) {
                    e_dct_links_features.close();
                }
                if (e_main_links_features != null) {
                    e_main_links_features.close();
                }
                if (e_sub_links_features != null) {
                    e_sub_links_features.close();
                }
                if (e_t_links_features_key != null) {
                    e_t_links_features_key.close();
                }
                if (e_dct_links_features_key != null) {
                    e_dct_links_features_key.close();
                }
                if (e_main_links_features_key != null) {
                    e_main_links_features_key.close();
                }
                if (e_sub_links_features_key != null) {
                    e_sub_links_features_key.close();
                }
            }



        } catch (Exception e) {
            System.err.println("Errors found (TempEval):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    /**
     * Obtains the dataset of a directory containing tml files.
     * @param basedir
     * @param approach
     * @param lang
     */
    public static void tmldir2features(File basedir, String approach, String lang) {
        String featuresdir = basedir.getParent() + File.separator + basedir.getName() + "_" + approach + "_features" + File.separator;
        try {
            if ((new File(featuresdir + "base-segmentation.TempEval2-features").exists())) {
                throw new Exception("PREVENTIVE ERROR: Save or delete the previousely generated features because these will be overwritten after this process: " + featuresdir);
            }

            File ftdir = new File(featuresdir);
            if (ftdir.exists()) {
                FileUtils.deleteRecursively(ftdir);
            }
            if (!ftdir.mkdirs()) {  // mkdirs creates many parent dirs if needed
                throw new Exception("Directory not created...");
            }

            File[] tmlfiles = basedir.listFiles(FileUtils.onlyFilesFilter);
            File[] tmldirs = basedir.listFiles(FileUtils.onlyDirsNonAuxDirs);

            for (int i = 0; i < tmldirs.length; i++) {
                tmlfiles = StringUtils.concatArray(tmlfiles, tmldirs[i].listFiles(FileUtils.onlyFilesFilter));
            }

            for (File tmlfile : tmlfiles) {
                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.println("File: " + tmlfile.getAbsolutePath());
                }
                NLPFile nlpfile = new PlainFile(tmlfile.getAbsolutePath());
                nlpfile.setLanguage(lang);
                if (!(FileUtils.getNLPFormat(nlpfile.getFile())).equalsIgnoreCase("XML")) {
                    throw new Exception("TimeML (.tml) XML file is required as input. Found: " + nlpfile.getFile().getCanonicalPath());
                }

                XMLFile xmlfile = new XMLFile(nlpfile.getFile().getAbsolutePath(),null);
                xmlfile.setLanguage(lang);
                if (!xmlfile.getExtension().equalsIgnoreCase("tml")) {
                    throw new Exception("TimeML (.tml) XML file is required as input.");
                }

                if (!xmlfile.isWellFormatted()) {
                    throw new Exception("File: " + xmlfile.getFile() + " is not a valid TimeML (.tml) XML file.");
                }



                // Create a working directory (commented because that way we can reuse roth-freeling annotations)
                File dir = new File(nlpfile.getFile().getCanonicalPath() + "-" + approach + "_features/");
                if (!dir.exists() || !dir.isDirectory()) {
                    dir.mkdir();
                }
                // Copy the valid TML-XML file
                String output = dir + File.separator + nlpfile.getFile().getName();
                FileUtils.copyFileUtil(nlpfile.getFile(), new File(output));
                xmlfile=new XMLFile(output,null);


                // get plain
                String plainfile = xmlfile.toPlain(output+".plain");
                String features = null;
                features = BaseTokenFeatures.getFeatures4Plain(lang, plainfile, 1, false, "TempEval2-features", approach);
                FileConverter.tmlfile2features4training(xmlfile, features);

                // MERGE

                // dir/base-segmentation.tab --> add to base-segmentation.tab
                FileUtils.copyFileUtilappend(new File(dir + "/base-segmentation.tab"), new File(featuresdir + "base-segmentation.tab"));

                // add TempEval2 features
                FileUtils.copyFileUtilappend(new File(dir + File.separator + tmlfile.getName() + ".plain.TempEval2-features"), new File(featuresdir + "base-segmentation.TempEval2-features"));

                // add to dct tab
                FileUtils.copyFileUtilappend(new File(dir + "/dct.tab"), new File(featuresdir + "dct.tab"));

                // add to timex-extents
                FileUtils.copyFileUtilappend(new File(dir + "/timex-extents.tab"), new File(featuresdir + "timex-extents.tab"));

                // add to timex-attributes
                FileUtils.copyFileUtilappend(new File(dir + "/timex-attributes.tab"), new File(featuresdir + "timex-attributes.tab"));

                // add to event-extents
                FileUtils.copyFileUtilappend(new File(dir + "/event-extents.tab"), new File(featuresdir + "event-extents.tab"));

                // add to event-attributes
                FileUtils.copyFileUtilappend(new File(dir + "/event-attributes.tab"), new File(featuresdir + "event-attributes.tab"));


                // add to link features
                FileUtils.copyFileUtilappend(new File(dir + File.separator + tmlfile.getName() + ".plain.e-t-link-features"), new File(featuresdir + "base-segmentation.e-t-link-features"));
                FileUtils.copyFileUtilappend(new File(dir + File.separator + tmlfile.getName() + ".plain.e-dct-link-features"), new File(featuresdir + "base-segmentation.e-dct-link-features"));
                FileUtils.copyFileUtilappend(new File(dir + File.separator + tmlfile.getName() + ".plain.e-main-link-features"), new File(featuresdir + "base-segmentation.e-main-link-features"));
                FileUtils.copyFileUtilappend(new File(dir + File.separator + tmlfile.getName() + ".plain.e-sub-link-features"), new File(featuresdir + "base-segmentation.e-sub-link-features"));
                FileUtils.copyFileUtilappend(new File(dir + File.separator + tmlfile.getName() + ".plain.e-t-link-features-annotationKey"), new File(featuresdir + "base-segmentation.e-t-link-features-annotationKey"));
                FileUtils.copyFileUtilappend(new File(dir + File.separator + tmlfile.getName() + ".plain.e-dct-link-features-annotationKey"), new File(featuresdir + "base-segmentation.e-dct-link-features-annotationKey"));
                FileUtils.copyFileUtilappend(new File(dir + File.separator + tmlfile.getName() + ".plain.e-main-link-features-annotationKey"), new File(featuresdir + "base-segmentation.e-main-link-features-annotationKey"));
                FileUtils.copyFileUtilappend(new File(dir + File.separator + tmlfile.getName() + ".plain.e-sub-link-features-annotationKey"), new File(featuresdir + "base-segmentation.e-sub-link-features-annotationKey"));

                // delete features folder
                // maintain features // FileUtils.deleteRecursively(dir);


            }
        } catch (Exception e) {
            System.err.println("Errors found (FileConverter):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }

    }



    /**
     * Returns a Hash with [value,Timex] pairs from a dct.tab
     *
     * @param dctsTabFile
     * @return
     */
    public static HashMap<String, Timex> getTimexDCTsFromTab(String dctsTabFile) {
        HashMap<String, Timex> DCTs = null;
        try {
            if (!(new File(dctsTabFile)).exists()) {
                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.println(dctsTabFile + " does not exist.");
                }
                return null;
            }
            BufferedReader dctreader = new BufferedReader(new FileReader(dctsTabFile));
            try {
                String line;
                DCTs = new HashMap<String, Timex>();
                while ((line = dctreader.readLine()) != null) {
                    String[] linearr = line.split("\t");
                    if (linearr[1].matches("[0-9]{8}")) {
                        linearr[1] = linearr[1].substring(0, 4) + "-" + linearr[1].substring(4, 6) + "-" + linearr[1].substring(6, 8);
                    }
                    if (linearr.length == 2) {
                        DCTs.put(linearr[0], new Timex("t0", linearr[1],"DATE",linearr[1],linearr[0],-1,-1,true));
                    }
                    if (linearr.length == 3) {
                        DCTs.put(linearr[0], new Timex(linearr[2], linearr[1],"DATE",linearr[1],linearr[0],-1,-1,true));
                    }
                }
            } finally {
                if (dctreader != null) {
                    dctreader.close();
                }
            }
        } catch (Exception e) {
            System.err.println("Errors found (TempEvalFiles):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
            return null;
        }
        return DCTs;
    }

}
