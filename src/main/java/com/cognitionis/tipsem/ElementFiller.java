package com.cognitionis.tipsem;

import com.cognitionis.nlp_files.PipesFile;
import com.cognitionis.nlp_files.parentical_parsers.*;
import com.cognitionis.timeml_basickit.*;
import com.cognitionis.utils_basickit.XmlAttribs;
import java.io.*;
import java.util.*;

/**
 * @author Hector Llorens
 * @since 2011
 */
public class ElementFiller {

    /**
     * Extracts the timexes, events, makeinstances and links of a file with all their features (if available).
     *
     * The function iterates through sentences extracting these elements and for each sentence calculates its temporal relations.
     *
     * @param features_annotated
     * @param DCTs
     * @param timexes
     * @param events
     * @param links
     * @param ommit_re
     */
    public static void get_elements(PipesFile features_annotated, HashMap<String, Timex> DCTs, HashMap<String, HashMap<String, Timex>> timexes, HashMap<String, HashMap<String, Event>> events, HashMap<String, HashMap<String, Event>> makeinstances, HashMap<String, HashMap<String, Link>> links, String ommit_re) {
        int linen = 0;

        try {
            int iob2col = features_annotated.getColumn("element\\(IOB2\\)");
            int attrscol = iob2col + 1;
            int wordcolumn = features_annotated.getColumn("word");
            int syntcolumn = features_annotated.getColumn("synt");
            int phrasecol = features_annotated.getColumn("phra_id");
            int govPrepcol = features_annotated.getColumn("PPdetail");
            int rolecolumn = features_annotated.getColumn("simpleroles");
            Element inElem = null;
            SyntColParser sbarparser = null;
            SyntColSBarTMPRoleParser sbarroleparser = null;


            Timex dct_timex = null;
            String lastReference = null;
            String lastMainEvent = null;
            String[] mainEvent = {"", "1000"}; // makeinstanceid and syntax level

            ArrayList<String[]> sentArray = null;
            ArrayList<String> sentTimexes = null;
            ArrayList<String> sentMakeinstances = null;

            HashMap<String, Timex> doc_timexes = null;
            HashMap<String, Event> doc_events = null;
            HashMap<String, Event> doc_makeinstances = null;
            HashMap<String, Link> doc_links = null;

            BufferedReader pipesreader = new BufferedReader(new FileReader(features_annotated.getFile()));
            String basefile = features_annotated.getFile().getCanonicalPath().substring(0, features_annotated.getFile().getCanonicalPath().indexOf(".TempEval2-features"));
            BufferedWriter e_t_links_features = new BufferedWriter(new FileWriter(new File(basefile + ".e-t-link-features")));
            BufferedWriter e_dct_links_features = new BufferedWriter(new FileWriter(new File(basefile + ".e-dct-link-features")));
            BufferedWriter e_main_links_features = new BufferedWriter(new FileWriter(new File(basefile + ".e-main-link-features")));
            BufferedWriter e_sub_links_features = new BufferedWriter(new FileWriter(new File(basefile + ".e-sub-link-features")));

            try {
                String pipesline;
                String[] pipesarr = null;
                String curr_docid = "";
                String curr_sentN = "";

                while ((pipesline = pipesreader.readLine()) != null) {
                    linen++;
                    pipesarr = pipesline.split("\\|");
                    //System.out.println(linen+"  "+pipesline);
                    if (!curr_docid.equals(pipesarr[0]) || !curr_sentN.equals(pipesarr[1])) {
                        // Add inElem elements before process
                        if (inElem != null && inElem.getClass().getSimpleName().equals("Timex")) {
                            if (ommit_re != null && ((Timex) inElem).get_expression().matches(ommit_re)) {
                                ((Timex) inElem).set_type("Omitted");
                                ((Timex) inElem).set_value("Omitted");
                                System.out.println("omit: " + ((Timex) inElem).get_expression());
                            }
                            if (((Timex) inElem).isReference()) {
                                lastReference = ((Timex) inElem).get_id();

                            }
                            doc_timexes.put(inElem.get_id(), (Timex) inElem);
                            sentTimexes.add(inElem.get_id());
                        }
                        if (inElem != null && inElem.getClass().getSimpleName().equals("Event")) {
                            doc_events.put(inElem.get_id(), (Event) inElem);
                            doc_makeinstances.put("ei" + inElem.get_id().substring(1), (Event) inElem);
                            sentMakeinstances.add("ei" + inElem.get_id().substring(1));
                        }
                        inElem = null;
                        if (sentArray != null) {
                            // set main event
                            if (!mainEvent[0].equals("")) {
                                doc_makeinstances.get(mainEvent[0]).set_is_main(true);
                            }
                            // check sentence elements
                            simpleProcessSentence(curr_docid, sentTimexes, sentMakeinstances, sentArray, lastReference, lastMainEvent, dct_timex, doc_timexes, doc_makeinstances, doc_links, e_t_links_features, e_dct_links_features, e_main_links_features, e_sub_links_features);
                        }

                        // initialize sentence
                        curr_sentN = pipesarr[1];
                        sentArray = null;
                        sentArray = new ArrayList<String[]>();
                        sentTimexes = null;
                        sentTimexes = new ArrayList<String>();
                        sentMakeinstances = null;
                        sentMakeinstances = new ArrayList<String>();
                        sbarparser = null;
                        sbarparser = new SyntColParser();
                        sbarroleparser = null;
                        sbarroleparser = new SyntColSBarTMPRoleParser();

                        lastMainEvent = mainEvent[0]; // previous main event update
                        mainEvent[0] = lastMainEvent; // That way if there is no event in the sentence it keeps the last one
                        mainEvent[1] = "1000";

                        // initialize document
                        if (!curr_docid.equals(pipesarr[0])) {
                            // save previous
                            if (!curr_docid.equals("")) {
                                timexes.put(curr_docid, doc_timexes);
                                events.put(curr_docid, doc_events);
                                makeinstances.put(curr_docid, doc_makeinstances);
                                links.put(curr_docid, doc_links);
                            }
                            // create new ones
                            curr_docid = pipesarr[0];
                            doc_timexes = null;
                            doc_events = null;
                            doc_makeinstances = null;
                            doc_links = null;
                            lastReference = null;
                            lastMainEvent = null;
                            dct_timex = null;
                            if (DCTs.containsKey(curr_docid)) {
                                dct_timex = DCTs.get(curr_docid);
                                lastReference = dct_timex.get_id();
                            }
                            doc_timexes = new HashMap<String, Timex>();
                            doc_events = new HashMap<String, Event>();
                            doc_makeinstances = new HashMap<String, Event>();
                            doc_links = new HashMap<String, Link>();
                        }

                    }

                    sentArray.add(pipesarr);

                    // Synt
                    boolean hasClosingBrakets = false;
                    if (pipesarr[syntcolumn].indexOf(')') != -1) {
                        hasClosingBrakets = true;
                    }
                    if (hasClosingBrakets) {
                        sbarparser.parse(pipesarr[syntcolumn].substring(0, pipesarr[syntcolumn].indexOf(')')));
                        sbarroleparser.parse(pipesarr[syntcolumn].substring(0, pipesarr[syntcolumn].indexOf(')')), pipesarr[rolecolumn], pipesarr[wordcolumn]);
                    } else {
                        sbarparser.parse(pipesarr[syntcolumn]);
                        sbarroleparser.parse(pipesarr[syntcolumn], pipesarr[rolecolumn], pipesarr[wordcolumn]);
                    }

                    if (pipesarr[iob2col].startsWith("B")) {
                        // Add current elem if exists
                        if (inElem != null) {
                            if (inElem != null && inElem.getClass().getSimpleName().equals("Timex")) {
                                if (ommit_re != null && ((Timex) inElem).get_expression().matches(ommit_re)) {
                                    ((Timex) inElem).set_type("Omitted");
                                    ((Timex) inElem).set_value("Omitted");
                                    System.out.println("omit: " + ((Timex) inElem).get_expression());
                                }
                                if ((((Timex) inElem).get_type().equalsIgnoreCase("date") || ((Timex) inElem).get_type().equalsIgnoreCase("time")) && ((Timex) inElem).get_value().matches("[0-9]{4}(-.*)?")) {
                                    lastReference = ((Timex) inElem).get_id();
                                }
                                doc_timexes.put(inElem.get_id(), (Timex) inElem);
                                sentTimexes.add(inElem.get_id());
                            }
                            if (inElem.getClass().getSimpleName().equals("Event")) {
                                doc_events.put(inElem.get_id(), (Event) inElem);
                                doc_makeinstances.put("ei" + inElem.get_id().substring(1), (Event) inElem);
                                sentMakeinstances.add("ei" + inElem.get_id().substring(1));
                            }
                            inElem = null;
                        }

                        // Start new element
                        HashMap<String, String> attrs = XmlAttribs.parseAttrs(pipesarr[attrscol]);
                        String element = pipesarr[iob2col].substring(2);
                        if (element.equalsIgnoreCase("event")) {
                            String id = attrs.get("eid");
                            String event_class = attrs.get("class");
                            inElem = new Event(id, pipesarr[3], event_class, curr_docid, Integer.parseInt(curr_sentN), Integer.parseInt(pipesarr[2].substring(0, pipesarr[2].indexOf('-'))));
                            ((Event) inElem).set_pos(Event.treebank2tml_pos(pipesarr[4]));
                            
                            ((Event) inElem).set_tense_aspect_modality(pipesarr[features_annotated.getColumn("tense")],pipesarr[features_annotated.getColumn("pos")]);
                            ((Event) inElem).set_polarity(pipesarr[features_annotated.getColumn("assertype")]);
                        } else {
                            if (element.equalsIgnoreCase("timex") || element.equalsIgnoreCase("timex3")) {
                                String id = attrs.get("tid");
                                String type = attrs.get("type");
                                String value = attrs.get("value");
                                inElem = new Timex(id, pipesarr[3], type, value, curr_docid, Integer.parseInt(curr_sentN), Integer.parseInt(pipesarr[2].substring(0, pipesarr[2].indexOf('-'))));
                            }
                        }
                        //subsent and phrase
                        if (inElem != null) {
                            //System.out.println(pipesarr[phrasecol]);
                            inElem.set_phra_id(pipesarr[phrasecol]);
                            inElem.set_subsent_num(sbarparser.getCurrentSubsent());
                            inElem.set_syntLevel(sbarparser.getParlevel());
                            inElem.set_govPrep(pipesarr[govPrepcol]);
                            inElem.set_govTMPSub(sbarroleparser.getSubsentTMP());
                            if (inElem.getClass().getSimpleName().equals("Event")) {
                                if (sbarparser.getParlevel() < Integer.parseInt(mainEvent[1]) || (sbarparser.getParlevel() == Integer.parseInt(mainEvent[1]) && doc_makeinstances.get(mainEvent[0]).get_class().equalsIgnoreCase("ASPECTUAL") && !((Event) inElem).get_class().equalsIgnoreCase("ASPECTUAL"))) {
                                    mainEvent[0] = "ei" + inElem.get_id().substring(1);
                                    mainEvent[1] = "" + sbarparser.getParlevel();
                                }
                            }
                        }
                    }

                    // IMP: SIGNALS ARE OUTSIDE
                    if (pipesarr[iob2col].startsWith("I") && !pipesarr[iob2col].equalsIgnoreCase("I-SIGNAL")) {
                        if (inElem == null) {
                            System.err.println("Found I-X element without B-X (OMMITED I-X) - line: "+linen);
                        }else{
                            inElem.extend_element(pipesarr[wordcolumn]);
                        }
                    }
                    if (pipesarr[iob2col].equals("O")) {
                        if (inElem != null && inElem.getClass().getSimpleName().equals("Timex")) {
                            if (ommit_re != null && ((Timex) inElem).get_expression().matches(ommit_re)) {
                                ((Timex) inElem).set_type("Omitted");
                                ((Timex) inElem).set_value("Omitted");
                                System.out.println("omit: " + ((Timex) inElem).get_expression());
                            }
                            if (((Timex) inElem).isReference()) {
                                lastReference = ((Timex) inElem).get_id();
                            }
                            doc_timexes.put(inElem.get_id(), (Timex) inElem);
                            sentTimexes.add(inElem.get_id());
                        }
                        if (inElem != null && inElem.getClass().getSimpleName().equals("Event")) {
                            doc_events.put(inElem.get_id(), (Event) inElem);
                            doc_makeinstances.put("ei" + inElem.get_id().substring(1), (Event) inElem);
                            sentMakeinstances.add("ei" + inElem.get_id().substring(1));
                        }
                        inElem = null;
                    }

                    // Synt
                    if (hasClosingBrakets) {
                        sbarparser.parse(pipesarr[syntcolumn].substring(pipesarr[syntcolumn].indexOf(')')));
                        sbarroleparser.parse(pipesarr[syntcolumn].substring(pipesarr[syntcolumn].indexOf(')')), pipesarr[rolecolumn], pipesarr[wordcolumn]);
                    }


                }

                if (inElem != null) {
                    if (inElem != null && inElem.getClass().getSimpleName().equals("Timex")) {
                        if (ommit_re != null && ((Timex) inElem).get_expression().matches(ommit_re)) {
                            ((Timex) inElem).set_type("Omitted");
                            ((Timex) inElem).set_value("Omitted");
                            System.out.println("omit: " + ((Timex) inElem).get_expression());
                        }
                        if (((Timex) inElem).isReference()) {
                            lastReference = ((Timex) inElem).get_id();
                        }
                        doc_timexes.put(inElem.get_id(), (Timex) inElem);
                        sentTimexes.add(inElem.get_id());
                    }
                    if (inElem.getClass().getSimpleName().equals("Event")) {
                        doc_events.put(inElem.get_id(), (Event) inElem);
                        doc_makeinstances.put("ei" + inElem.get_id().substring(1), (Event) inElem);
                        sentMakeinstances.add("ei" + inElem.get_id().substring(1));
                    }
                    inElem = null;
                }

                // set main event
                if (!mainEvent[0].equals("")) {
                    doc_makeinstances.get(mainEvent[0]).set_is_main(true);
                }
                // check sentElements
                simpleProcessSentence(curr_docid, sentTimexes, sentMakeinstances, sentArray, lastReference, lastMainEvent, dct_timex, doc_timexes, doc_makeinstances, doc_links, e_t_links_features, e_dct_links_features, e_main_links_features, e_sub_links_features);

                // add the last file elements
                timexes.put(curr_docid, doc_timexes);
                events.put(curr_docid, doc_events);
                makeinstances.put(curr_docid, doc_makeinstances);
                links.put(curr_docid, doc_links);


            } finally {
                if (pipesreader != null) {
                    pipesreader.close();
                }
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
            }

        } catch (Exception e) {
            System.err.println("Errors found (TempEval):\n\t" + e.toString() + " (Reading line " + linen + " " + features_annotated.getFile() + ")\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    /**
     * Updates the links of a file with the categories.
     * @param et
     * @param edct
     * @param emain
     * @param esub
     * @param links
     */
    public static void updateLinks(String et, String edct, String emain, String esub, HashMap<String, HashMap<String, Link>> links) {
        int linen = 0;

        try {
            BufferedReader etreader = new BufferedReader(new FileReader(new File(et)));
            BufferedReader edctreader = new BufferedReader(new FileReader(new File(edct)));
            BufferedReader emainreader = new BufferedReader(new FileReader(new File(emain)));
            BufferedReader esubreader = new BufferedReader(new FileReader(new File(esub)));

            try {
                String pipesline;
                String[] pipesarr = null;

                while ((pipesline = etreader.readLine()) != null) {
                    linen++;
                    pipesarr = pipesline.split("\\|");
                    links.get(pipesarr[0]).get(pipesarr[1]).set_category(pipesarr[pipesarr.length - 1]);
                }
                while ((pipesline = edctreader.readLine()) != null) {
                    linen++;
                    pipesarr = pipesline.split("\\|");
                    links.get(pipesarr[0]).get(pipesarr[1]).set_category(pipesarr[pipesarr.length - 1]);
                }
                while ((pipesline = emainreader.readLine()) != null) {
                    linen++;
                    pipesarr = pipesline.split("\\|");
                    links.get(pipesarr[0]).get(pipesarr[1]).set_category(pipesarr[pipesarr.length - 1]);
                }
                while ((pipesline = esubreader.readLine()) != null) {
                    linen++;
                    pipesarr = pipesline.split("\\|");
                    links.get(pipesarr[0]).get(pipesarr[1]).set_category(pipesarr[pipesarr.length - 1]);
                }

            } finally {
                if (etreader != null) {
                    etreader.close();
                }
                if (edctreader != null) {
                    edctreader.close();
                }
                if (emainreader != null) {
                    emainreader.close();
                }
                if (esubreader != null) {
                    esubreader.close();
                }
            }

        } catch (Exception e) {
            System.err.println("Errors found (TempEval):\n\t" + e.toString() + " (Reading line " + linen + ")\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    /**
     * Given a sentence with its timexes and events and their synt properties and it calclates the TLINKs.
     * It includes:
     *     -event-timex
     *     -event-dct
     *     -event-event main (previous sentence or last main event)
     *     -event-event sub (intra sentential)
     *
     * Furthermore it writes the feature files for each kind of relations
     *
     * @param timexes
     * @param makeinstances
     * @param sentence
     * @param lastMainEvent
     * @param links
     * @throws Exception
     */
    public static void simpleProcessSentence(String docid, ArrayList<String> sentTimexes, ArrayList<String> sentMakeinstances, ArrayList<String[]> sentence, String lastReference, String lastMainEvent, Timex dct_timex, HashMap<String, Timex> timexes, HashMap<String, Event> makeinstances, HashMap<String, Link> links, BufferedWriter e_t_links_features, BufferedWriter e_dct_links_features, BufferedWriter e_main_links_features, BufferedWriter e_sub_links_features) throws Exception {
        if (sentMakeinstances.size() > 0) {
            // find event-timex
            for (String tid : sentTimexes) {
                String syntrelation = "none";
                String related_eventinstance = null;
                for (String eiid : sentMakeinstances) {
                    if (makeinstances.get(eiid).get_phra_id().equals(timexes.get(tid).get_phra_id())) {
                        syntrelation = "samephra";
                        if (related_eventinstance == null) {
                            related_eventinstance = eiid;
                        } else {
                            if (Math.abs(makeinstances.get(related_eventinstance).get_tok_num() - timexes.get(tid).get_tok_num()) > Math.abs(makeinstances.get(eiid).get_tok_num() - timexes.get(tid).get_tok_num())) {
                                related_eventinstance = eiid;
                            }
                        }
                    }
                }
                if (related_eventinstance == null) {
                    for (String eiid : sentMakeinstances) {
                        if (makeinstances.get(eiid).get_subsent_id().equals(timexes.get(tid).get_subsent_id())) {
                            syntrelation = "samesubsent";
                            if (related_eventinstance == null) {
                                related_eventinstance = eiid;
                            } else {
                                if (Math.abs(makeinstances.get(related_eventinstance).get_tok_num() - timexes.get(tid).get_tok_num()) > Math.abs(makeinstances.get(eiid).get_tok_num() - timexes.get(tid).get_tok_num())) {
                                    related_eventinstance = eiid;
                                }
                            }
                        }
                    }
                }
                if (related_eventinstance == null) {
                    for (String eiid : sentMakeinstances) {
                        syntrelation = "samesent";
                        if (related_eventinstance == null) {
                            related_eventinstance = eiid;
                        } else {
                            if (Math.abs(makeinstances.get(related_eventinstance).get_tok_num() - timexes.get(tid).get_tok_num()) > Math.abs(makeinstances.get(eiid).get_tok_num() - timexes.get(tid).get_tok_num())) {
                                related_eventinstance = eiid;
                            }
                        }
                    }
                }
                Link l = null;
                if (timexes.get(tid).isReference()) {
                    if (makeinstances.get(related_eventinstance).is_linked_to_a_ref()) {
                        // create new makeinstance
                        int tempid = Integer.parseInt(related_eventinstance.substring(2));
                        String newid = "ei" + Event.firstExtraMakeinstanceId.substring(0, Event.firstExtraMakeinstanceId.length() - ("" + tempid).length()) + tempid;
                        while (makeinstances.containsKey(newid)) {
                            newid = "ei" + Event.firstExtraMakeinstanceId.substring(0, Event.firstExtraMakeinstanceId.length() - ("" + tempid).length()) + tempid;
                            tempid++;
                        }
                        Event auxe = makeinstances.get(related_eventinstance).clone();
                        auxe.set_eiid(newid);
                        makeinstances.put(newid, auxe);
                        sentMakeinstances.add(newid);
                        related_eventinstance = newid;
                    }
                    l = new Link("l" + (links.size() + 1), "tlink-event-timex-ref", "NONE", related_eventinstance, tid, docid);
                    makeinstances.get(related_eventinstance).set_is_linked_to_ref(true);
                } else {
                    l = new Link("l" + (links.size() + 1), "tlink-event-timex-" + timexes.get(tid).get_type() + "-not-ref", "NONE", related_eventinstance, tid, docid);
                }
                links.put(l.get_id(), l);

                String tref = timexes.get(tid).get_value();
                if (timexes.get(tid).isReference()) {
                    tref = "reference";
                }
                if (!timexes.get(tid).get_type().matches("(TIME|DATE)")) {
                    tref = "duration-set";
                }
                //file|lid|eiid|tid|e_class|e_pos|e_token|e_tense|e_tense-aspect|e_govPP|e_govTMPSub|t_type|t_ref|t_govPP|t_govTMPSub|synt_relation
                e_t_links_features.write(docid + "|" + "l" + links.size() + "|" + related_eventinstance + "|" + tid + "|" + makeinstances.get(related_eventinstance).get_class() + "|" + makeinstances.get(related_eventinstance).get_POS() + "|" + makeinstances.get(related_eventinstance).get_expression() + "|" + makeinstances.get(related_eventinstance).get_tense() + "|" + makeinstances.get(related_eventinstance).get_tense() + "|" + makeinstances.get(related_eventinstance).get_tense() + "-" + makeinstances.get(related_eventinstance).get_aspect() + "|" + makeinstances.get(related_eventinstance).get_govPrep() + "|" + makeinstances.get(related_eventinstance).get_govTMPSub() + "|" + timexes.get(tid).get_type() + "|" + tref + "|" + timexes.get(tid).get_govPrep() + "|" + timexes.get(tid).get_govTMPSub() + "|" + syntrelation + "\n");

            }



            // find if there is a dct or reference time
            String dct_or_ref_id = null;
            if (dct_timex != null) {
                dct_or_ref_id = dct_timex.get_id();
            } else {
                if (lastReference != null) {
                    dct_or_ref_id = lastReference;
                }
            }

            // find event-dct while finding one main event
            String main_event = null;
            for (String eiid : sentMakeinstances) {
                if (main_event == null && makeinstances.get(eiid).is_main()) {
                    main_event = eiid;
                } else {
                    if (dct_or_ref_id != null && makeinstances.get(eiid).get_class().equalsIgnoreCase("REPORTING")) {
                        Link l = new Link("l" + (links.size() + 1), "tlink-event-timex-dct_or_ref-reporting", "NONE", eiid, dct_or_ref_id, docid);
                        links.put(l.get_id(), l);
                        //file|lid|eiid|tid|e_class|e_pos|e_token|e_tense|e_tense-aspect|e_govPP|e_govTMPSub|gov_e_class|gov_e_pos|gov_e_token|gov_e_tense-aspect
                        e_dct_links_features.write(docid + "|" + "l" + links.size() + "|" + eiid + "|" + dct_or_ref_id + "|" + makeinstances.get(eiid).get_class() + "|" + makeinstances.get(eiid).get_POS() + "|" + makeinstances.get(eiid).get_expression() + "|" + makeinstances.get(eiid).get_tense() + "|" + makeinstances.get(eiid).get_tense() + "-" + makeinstances.get(eiid).get_aspect() + "|" + makeinstances.get(eiid).get_govPrep() + "|" + makeinstances.get(eiid).get_govTMPSub() + "|gov_e_class|gov_e_pos|gov_e_token|gov_e_tense-aspect\n");

                    }
                }
            }

            if (main_event == null) {
                throw new Exception("Main event not found near " + sentMakeinstances.get(0));
            }

            // link main to dct
            if (dct_or_ref_id != null) {
                Link l = new Link("l" + (links.size() + 1), "tlink-event-timex-dct_or_ref", "NONE", main_event, dct_or_ref_id, docid);
                links.put(l.get_id(), l);
                //file|lid|eiid|tid|e_class|e_pos|e_token|e_tense|e_tense-aspect|e_govPP|e_govTMPSub|gov_e_class|gov_e_pos|gov_e_token|gov_e_tense-aspect
                e_dct_links_features.write(docid + "|" + "l" + links.size() + "|" + main_event + "|" + dct_or_ref_id + "|" + makeinstances.get(main_event).get_class() + "|" + makeinstances.get(main_event).get_POS() + "|" + makeinstances.get(main_event).get_expression() + "|" + makeinstances.get(main_event).get_tense() + "|" + makeinstances.get(main_event).get_tense() + "-" + makeinstances.get(main_event).get_aspect() + "|" + makeinstances.get(main_event).get_govPrep() + "|" + makeinstances.get(main_event).get_govTMPSub() + "|gov_e_class|gov_e_pos|gov_e_token|gov_e_tense-aspect\n");
            }

            // link main to previous main
            if (lastMainEvent != null && !lastMainEvent.equals("")) {
                Link l = new Link("l" + (links.size() + 1), "tlink-main-event", "NONE", lastMainEvent, main_event, docid);
                links.put(l.get_id(), l);
                // file|lid|eiid1|eiid2|e1_class|e1_pos|e1_token|e1_tense-aspect|e2_class|e2_pos|e2_token|e2_tense-aspect
                e_main_links_features.write(docid + "|" + "l" + links.size() + "|" + lastMainEvent + "|" + main_event + "|" + makeinstances.get(lastMainEvent).get_class() + "|" + makeinstances.get(lastMainEvent).get_POS() + "|" + makeinstances.get(lastMainEvent).get_expression() + "|" + makeinstances.get(lastMainEvent).get_tense() + "|" + makeinstances.get(lastMainEvent).get_tense() + "-" + makeinstances.get(lastMainEvent).get_aspect() + "|" + makeinstances.get(main_event).get_class() + "|" + makeinstances.get(main_event).get_POS() + "|" + makeinstances.get(main_event).get_expression() + "|" + makeinstances.get(main_event).get_tense() + "|" + makeinstances.get(main_event).get_tense() + "-" + makeinstances.get(main_event).get_aspect() + "\n");
            }

            // Link non-mainto main --- sub-event (eiids in order)
            for (String eiid : sentMakeinstances) {
                boolean main_found = false;
                boolean reversed_relation = false;
                if (!main_event.equals(eiid)) {
                    Link l = null;
                    String syntrelation = "none";
                    if (makeinstances.get(eiid).get_phra_id().equals(makeinstances.get(main_event).get_phra_id())) {
                        syntrelation = "equal";
                    }
                    if (makeinstances.get(eiid).get_syntLevel() < makeinstances.get(main_event).get_syntLevel()) {
                        syntrelation = ">"; // inverse because the lower synt the more governing
                    } else {
                        if (makeinstances.get(eiid).get_syntLevel() > makeinstances.get(main_event).get_syntLevel()) {
                            syntrelation = "<"; // inverse because the lower synt the more governing
                        }
                    }
                    if (!main_found) {
                        l = new Link("l" + (links.size() + 1), "tlink-sub-event", "NONE", eiid, main_event, docid);
                        //file|lid|eiid1|eiid2|e1_class|e1_pos|e1_token|e1_tense|e1_tense-aspect|e1_govPP|e1_govTMPSub|e2_class|e2_pos|e2_token|e2_tense|e2_tense-aspect|e2_govPP|e2_govTMPSub|syntrel
                        e_sub_links_features.write(docid + "|" + "l" + (links.size() + 1) + "|" + eiid + "|" + main_event + "|" + makeinstances.get(eiid).get_class() + "|" + makeinstances.get(eiid).get_POS() + "|" + makeinstances.get(eiid).get_expression() + "|" + makeinstances.get(eiid).get_tense() + "|" + makeinstances.get(eiid).get_tense() + "-" + makeinstances.get(eiid).get_aspect() + "|" + makeinstances.get(eiid).get_govPrep() + "|" + makeinstances.get(eiid).get_govTMPSub() + "|" + makeinstances.get(main_event).get_class() + "|" + makeinstances.get(main_event).get_POS() + "|" + makeinstances.get(main_event).get_expression() + "|" + makeinstances.get(main_event).get_tense() + "|" + makeinstances.get(main_event).get_tense() + "-" + makeinstances.get(main_event).get_aspect() + "|" + makeinstances.get(main_event).get_govPrep() + "|" + makeinstances.get(main_event).get_govTMPSub() + "|" + syntrelation + "\n");
                    } else {
                        if (!reversed_relation) {
                            if (syntrelation.endsWith("<")) {
                                syntrelation = ">";
                            } else {
                                if (syntrelation.equals(">")) {
                                    syntrelation = "<";
                                }
                            }
                            reversed_relation = true;
                        }
                        l = new Link("l" + (links.size() + 1), "tlink-sub-event", "NONE", main_event, eiid, docid);
                        //file|lid|eiid1|eiid2|e1_class|e1_pos|e1_token|e1_tense|e1_tense-aspect|e1_govPP|e1_govTMPSub|e2_class|e2_pos|e2_token|e2_tense|e2_tense-aspect|e2_govPP|e2_govTMPSub|syntrel
                        e_sub_links_features.write(docid + "|" + "l" + (links.size() + 1) + "|" + main_event + "|" + eiid + "|" + makeinstances.get(main_event).get_class() + "|" + makeinstances.get(main_event).get_POS() + "|" + makeinstances.get(main_event).get_expression() + "|" + makeinstances.get(main_event).get_tense() + "|" + makeinstances.get(main_event).get_tense() + "-" + makeinstances.get(main_event).get_aspect() + "|" + makeinstances.get(main_event).get_govPrep() + "|" + makeinstances.get(main_event).get_govTMPSub() + "|" + makeinstances.get(eiid).get_class() + "|" + makeinstances.get(eiid).get_POS() + "|" + makeinstances.get(eiid).get_expression() + "|" + makeinstances.get(eiid).get_tense() + "|" + makeinstances.get(eiid).get_tense() + "-" + makeinstances.get(eiid).get_aspect() + "|" + makeinstances.get(eiid).get_govPrep() + "|" + makeinstances.get(eiid).get_govTMPSub() + "|" + syntrelation + "\n");
                    }
                    links.put(l.get_id(), l);
                } else {
                    main_found = true;
                }
            }



        }

        //manage_participants(makeinstances, sentence)

    }

    /**
     * Extracts the timexes and events of a file with all their features (if available).
     *
     * For makeinstances and temporal relations there could be another function
     * The function iterates through sentences extracting these elements and for each sentence calculates its temporal relations.
     *
     * @param features_annotated
     * @param timexes
     * @param events
     * @param links
     * @param ommit_re
     */
    public static void get_elements_old(PipesFile features_annotated, HashMap<String, Timex> timexes, HashMap<String, Event> events, HashMap<String, Link> links, String ommit_re) {
        int linen = 0;

        try {

            int iob2col = features_annotated.getColumn("element\\(IOB2\\)");
            int attrscol = iob2col + 1;
            int syntcolumn = features_annotated.getColumn("synt");
            int phrasecol = features_annotated.getColumn("phra_id");
            int wordcolumn = features_annotated.getColumn("word");

            Element inElem = null;
            SyntColParser sbarparser = null;


            String lastReference = "t0";
            ArrayList<String[]> sentArray = null;
            ArrayList<Timex> sentTimexes = null;
            ArrayList<Event> sentEvents = null;


            BufferedReader pipesreader = new BufferedReader(new FileReader(features_annotated.getFile()));

            try {
                String pipesline;
                String[] pipesarr = null;
                String curr_fileid = "";
                String curr_sentN = "";

                while ((pipesline = pipesreader.readLine()) != null) {
                    linen++;
                    pipesarr = pipesline.split("\\|");

                    //System.out.println(linen+"  "+pipesline);

                    if (!curr_fileid.equals(pipesarr[0]) || !curr_sentN.equals(pipesarr[1])) {
                        // Add inElem elements before process
                        if (inElem != null && inElem.getClass().getSimpleName().equals("Timex")) {
                            if (ommit_re != null && ((Timex) inElem).get_expression().matches(ommit_re)) {
                                ((Timex) inElem).set_type("Omitted");
                                ((Timex) inElem).set_value("Omitted");
                                System.out.println("omit: " + ((Timex) inElem).get_expression());
                            }
                            if ((((Timex) inElem).get_type().equalsIgnoreCase("date") || ((Timex) inElem).get_type().equalsIgnoreCase("time")) && ((Timex) inElem).get_value().matches("[0-9]{4}(-.*)?")) {
                                lastReference = ((Timex) inElem).get_id();
                            }
                            timexes.put(inElem.get_id(), (Timex) inElem);
                            sentTimexes.add((Timex) inElem);
                        }
                        if (inElem != null && inElem.getClass().getSimpleName().equals("Event")) {
                            events.put(inElem.get_id(), (Event) inElem);
                            sentEvents.add((Event) inElem);
                        }
                        inElem = null;
                        if (sentArray != null) {
                            processSentence(sentTimexes, sentEvents, sentArray, lastReference, links);
                        }
                        curr_sentN = pipesarr[1];
                        sentArray = null;
                        sentArray = new ArrayList<String[]>();
                        sentTimexes = null;
                        sentTimexes = new ArrayList<Timex>();
                        sentEvents = null;
                        sentEvents = new ArrayList<Event>();
                        sbarparser = null;
                        sbarparser = new SyntColParser();
                        if (!curr_fileid.equals(pipesarr[0])) {
                            curr_fileid = pipesarr[0];
                            lastReference = "t0";
                        }

                    }

                    sentArray.add(pipesarr);

                    // Synt
                    boolean hasClosingBrakets = false;
                    if (pipesarr[syntcolumn].indexOf(')') != -1) {
                        hasClosingBrakets = true;
                    }
                    if (hasClosingBrakets) {
                        sbarparser.parse(pipesarr[syntcolumn].substring(0, pipesarr[syntcolumn].indexOf(')')));
                    } else {
                        sbarparser.parse(pipesarr[syntcolumn]);
                    }

                    if (pipesarr[iob2col].startsWith("B")) {
                        // Add current elem if exists
                        if (inElem != null) {
                            if (inElem != null && inElem.getClass().getSimpleName().equals("Timex")) {
                                if (ommit_re != null && ((Timex) inElem).get_expression().matches(ommit_re)) {
                                    ((Timex) inElem).set_type("Omitted");
                                    ((Timex) inElem).set_value("Omitted");
                                    System.out.println("omit: " + ((Timex) inElem).get_expression());
                                }
                                if ((((Timex) inElem).get_type().equalsIgnoreCase("date") || ((Timex) inElem).get_type().equalsIgnoreCase("time")) && ((Timex) inElem).get_value().matches("[0-9]{4}(-.*)?")) {
                                    lastReference = ((Timex) inElem).get_id();
                                }
                                timexes.put(inElem.get_id(), (Timex) inElem);
                                sentTimexes.add((Timex) inElem);
                            }
                            if (inElem.getClass().getSimpleName().equals("Event")) {
                                events.put(inElem.get_id(), (Event) inElem);
                                sentEvents.add((Event) inElem);
                            }
                            inElem = null;
                        }

                        // Start new element
                        HashMap<String, String> attrs = XmlAttribs.parseSemiColonAttrs(pipesarr[attrscol]);
                        String element = pipesarr[iob2col].substring(2);
                        if (element.equalsIgnoreCase("event")) {
                            String id = attrs.get("eid");
                            String event_class = attrs.get("class");
                            inElem = new Event(id, pipesarr[3], event_class, curr_fileid, Integer.parseInt(curr_sentN), Integer.parseInt(pipesarr[2].substring(0, pipesarr[2].indexOf('-'))));
                        } else {
                            if (element.equalsIgnoreCase("timex") || element.equalsIgnoreCase("timex3")) {
                                String id = attrs.get("tid");
                                String type = attrs.get("type");
                                String value = attrs.get("value");
                                inElem = new Timex(id, pipesarr[3], type, value, curr_fileid, Integer.parseInt(curr_sentN), Integer.parseInt(pipesarr[2].substring(0, pipesarr[2].indexOf('-'))));
                            }
                        }
                        //subsent and phrase
                        if (inElem != null) {
                            //System.out.println(pipesarr[phrasecol]);
                            inElem.set_phra_id(pipesarr[phrasecol]);
                            inElem.set_subsent_num(sbarparser.getCurrentSubsent());
                        }
                    }

                    // IMP: SIGNALS ARE OUTSIDE
                    if (pipesarr[iob2col].startsWith("I") && !pipesarr[iob2col].equalsIgnoreCase("I-SIGNAL")) {
                        if (inElem == null) {
                            throw new Exception("Found I-X element without B-X");
                        }
                        inElem.extend_element(pipesarr[wordcolumn]);
                    }
                    if (pipesarr[iob2col].equals("O")) {
                        if (inElem != null && inElem.getClass().getSimpleName().equals("Timex")) {
                            if (ommit_re != null && ((Timex) inElem).get_expression().matches(ommit_re)) {
                                ((Timex) inElem).set_type("Omitted");
                                ((Timex) inElem).set_value("Omitted");
                                System.out.println("omit: " + ((Timex) inElem).get_expression());
                            }
                            if ((((Timex) inElem).get_type().equalsIgnoreCase("date") || ((Timex) inElem).get_type().equalsIgnoreCase("time")) && ((Timex) inElem).get_value().matches("[0-9]{4}(-.*)?")) {
                                lastReference = ((Timex) inElem).get_id();
                            }
                            timexes.put(inElem.get_id(), (Timex) inElem);
                            sentTimexes.add((Timex) inElem);
                        }
                        if (inElem != null && inElem.getClass().getSimpleName().equals("Event")) {
                            events.put(inElem.get_id(), (Event) inElem);
                            sentEvents.add((Event) inElem);
                        }
                        inElem = null;
                    }


                    // Synt
                    if (hasClosingBrakets) {
                        sbarparser.parse(pipesarr[syntcolumn].substring(pipesarr[syntcolumn].indexOf(')')));
                    }


                }

                if (inElem != null) {
                    if (inElem != null && inElem.getClass().getSimpleName().equals("Timex")) {
                        if (ommit_re != null && ((Timex) inElem).get_expression().matches(ommit_re)) {
                            ((Timex) inElem).set_type("Omitted");
                            ((Timex) inElem).set_value("Omitted");
                            System.out.println("omit: " + ((Timex) inElem).get_expression());
                        }
                        if ((((Timex) inElem).get_type().equalsIgnoreCase("date") || ((Timex) inElem).get_type().equalsIgnoreCase("time")) && ((Timex) inElem).get_value().matches("[0-9]{4}(-.*)?")) {
                            lastReference = ((Timex) inElem).get_id();
                        }
                        timexes.put(inElem.get_id(), (Timex) inElem);
                        sentTimexes.add((Timex) inElem);
                    }
                    if (inElem.getClass().getSimpleName().equals("Event")) {
                        events.put(inElem.get_id(), (Event) inElem);
                        sentEvents.add((Event) inElem);
                    }
                    inElem = null;
                }


                // check sentElements
                processSentence(sentTimexes, sentEvents, sentArray, lastReference, links);



            } finally {
                if (pipesreader != null) {
                    pipesreader.close();
                }
            }

        } catch (Exception e) {
            System.err.println("Errors found (TempEval):\n\t" + e.toString() + " (Reading line " + linen + " " + features_annotated.getFile() + ")\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    /**
     * Given a sentence with its timexes and events and their synt properties and the lastreference it calclates the TLINKs.
     * It includes:
     *     -event-timex date/times and duration relations.
     *     -event-event aspectual relations
     *
     *  Also handles a bit on participants.
     *
     * @param timexes
     * @param events
     * @param sentence
     * @param lastReference
     * @param links
     * @throws Exception
     */
    public static void processSentence(ArrayList<Timex> timexes, ArrayList<Event> events, ArrayList<String[]> sentence, String lastReference, HashMap<String, Link> links) throws Exception {
        ArrayList<Timex> timeRefs = new ArrayList<Timex>();
        ArrayList<Timex> durations = new ArrayList<Timex>();
        boolean other_than_aspectual = false;
        if (events.size() > 0) {
            if (timexes != null) {
                for (Timex t : timexes) {
                    if (t.isReference()) {
                        timeRefs.add(t);
                    }
                    if (t.get_type().equalsIgnoreCase("duration") && t.get_value().matches("P(T)?[X0-9]+[XYMWDHS0-9]+")) {
                        durations.add(t);
                    }
                }

            }

            if (events != null) {
                for (Event e : events) {
                    if (!((Event) e).get_class().equalsIgnoreCase("ASPECTUAL")) {
                        other_than_aspectual = true;
                    }
                }
            }


            find_event_links(events, sentence, lastReference, links, timeRefs, other_than_aspectual);
            manage_participants(events, sentence);
            if (durations.size() > 0) {
                find_duration_links(events, durations, sentence, links);
            }
        }

    }

    public static void find_event_links(ArrayList<Event> events, ArrayList<String[]> sentence, String lastReference, HashMap<String, Link> links, ArrayList<Timex> TimeRefs, boolean other_than_aspectual) throws Exception {

        ArrayList<Timex> unusedTimeRefs = (ArrayList<Timex>) TimeRefs.clone(); // Copy (not reference)

        //System.out.println("Before Refs="+TimeRefs.size()+" unused="+unusedTimeRefs.size());

        for (Event e : events) {
            Link l;
            String ssentence = "";
            // Look for ALINKS
            if (e.get_class().equalsIgnoreCase("aspectual") && other_than_aspectual) {
                Event aspectRel = null;

                for (Event e2 : events) {
                    if (!e2.get_class().equalsIgnoreCase("aspectual") && e2.get_phra_id().equals(e.get_phra_id())) {
                        aspectRel = e2;
                        break;
                    }
                }
                if (aspectRel == null) {
                    for (Event e2 : events) {
                        if (!e2.get_class().equalsIgnoreCase("aspectual") && e2.get_subsent_id().equals(e.get_subsent_id())) {
                            aspectRel = e2;
                            break;
                        }
                    }
                }
                if (aspectRel == null) {
                    for (Event e2 : events) {
                        if (!e2.get_class().equalsIgnoreCase("aspectual")) {
                            aspectRel = e2;
                            break;
                        }
                    }
                }
                if (aspectRel == null) {
                    throw new Exception("Aspectual link broken: " + e.get_doc_id() + " sent: " + e.get_sent_num());
                }
                l = new Link("l" + (links.size() + 1), "aspectual", "IDENTITY", e.get_id(), aspectRel.get_id(), e.get_doc_id());
                //System.out.println(e.get_expression() + " con event " + aspectRel.get_id());
                if (!aspectRel.get_aspectual_modifier().equals("")) {
                    aspectRel.set_aspectual_modifier(aspectRel.get_aspectual_modifier() + ", " + e.get_expression());
                } else {
                    aspectRel.set_aspectual_modifier(e.get_expression());
                }
                ssentence = "No context for aspectual events";


                // Otherwise look for TLINKS
            } else {
                String currentReference = null;
                if (TimeRefs.size() > 1) {
                    for (Timex t : TimeRefs) {
                        //System.out.println(TimeRefs.size()+"   timexid: "+t.get_id()+" eventid:"+e.get_id());
                        if (t.get_phra_id().equals(e.get_phra_id())) {
                            currentReference = t.get_id();
                            unusedTimeRefs.remove(t);
                            break;
                        }
                    }
                    if (currentReference == null) {
                        for (Timex t : TimeRefs) {
                            if (t.get_subsent_id().equals(e.get_subsent_id())) {
                                currentReference = t.get_id();
                                unusedTimeRefs.remove(t);
                                break;
                            }
                        }
                    }
                    // May be another option is calculate the distance difference
                }
                if (currentReference == null) {
                    currentReference = lastReference;
                }
                l = new Link("l" + (links.size() + 1), "tlink-event-timex", "IDENTITY", e.get_id(), currentReference, e.get_doc_id());
                // Full-CONTEXT sentence with event in bold
                int position = e.get_tok_num();
                for (int i = 0; i < sentence.size(); i++) {
                    if (i == position) {
                        ssentence += "<b>" + sentence.get(i)[3] + "</b> ";
                    } else {
                        ssentence += sentence.get(i)[3] + " ";
                    }
                }
            }
            e.set_context(ssentence);
            links.put(l.get_id(), l);
        }

        if (TimeRefs.size() > 1 && unusedTimeRefs.size() > 0) {
            //System.out.println("After Refs="+TimeRefs.size()+" unused="+unusedTimeRefs.size());
            Event newRel = null;
            for (Timex t : unusedTimeRefs) {
                //System.out.println(t.get_expression());
                Link l;
                for (Event e2 : events) {
                    if (!e2.get_class().equalsIgnoreCase("aspectual") && e2.get_phra_id().equals(t.get_phra_id())) {
                        newRel = e2;
                        break;
                    }
                }
                if (newRel == null) {
                    for (Event e2 : events) {
                        if (!e2.get_class().equalsIgnoreCase("aspectual") && e2.get_subsent_id().equals(t.get_subsent_id())) {
                            newRel = e2;
                            break;
                        }
                    }
                }
                // Otherwise use the first event
                if (newRel == null) {
                    newRel = events.get(0);
                }
                if (newRel == null) {
                    throw new Exception("Extra event-timex link broken: " + t.get_doc_id() + " sent: " + t.get_sent_num());
                }
                l = new Link("l" + (links.size() + 1), "tlink-event-timex", "IDENTITY", newRel.get_id(), t.get_id(), newRel.get_doc_id());
                links.put(l.get_id(), l);
            }
        }

    }

    public static void find_duration_links(ArrayList<Event> events, ArrayList<Timex> durations, ArrayList<String[]> sentence, HashMap<String, Link> links) throws Exception {
        for (Timex duration : durations) {
            //System.out.println("Duration: " + ((Timex) duration).get_expression());
            Event eventWithDuration = null;
            for (Event e : events) {
                if (e.get_phra_id().equals(duration.get_phra_id())) {
                    eventWithDuration = e;
                    break;
                }
            }
            if (eventWithDuration == null) {
                for (Event e : events) {
                    if (e.get_subsent_id().equals(duration.get_subsent_id())) {
                        eventWithDuration = e;
                        break;
                    }
                }
            }
            if (eventWithDuration == null) {
                for (Event e : events) {
                    eventWithDuration = e;
                    break;
                }
            }
            if (eventWithDuration == null) {
                if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                    System.err.println("Event-Duration link broken: " + duration.get_doc_id() + " sent: " + duration.get_sent_num());
                }
            } else {
                Link l = new Link("l" + (links.size() + 1), "tlink-event-duration", "IDENTITY", eventWithDuration.get_id(), duration.get_id(), duration.get_doc_id());
                links.put(l.get_id(), l);
                eventWithDuration.set_duration(duration.get_value());
            }
        }
    }

    public static void manage_participants(ArrayList<Event> events, ArrayList<String[]> sentence) {
        // Fill PARTICIPANTS
        String participants = "";

        // Better full sentence case... (normally this is better so...)
        //if (Math.floor(sentence.size() / 10) <= events.size()) {
        if (true) {
            int curr_event = 0;
            Event event = null;
            for (int i = 0; i < sentence.size(); i++) {
                while (curr_event < events.size() && event == null) {
                    event = events.get(curr_event);
                    curr_event++;
                }

                if (event != null && i == event.get_tok_num()) {
                    String tag = "b";
                    if (event.get_class().equalsIgnoreCase("ASPECTUAL")) {
                        tag = "i";
                    }
                    participants += " <" + tag + ">" + sentence.get(i)[3] + "</" + tag + ">";
                    event = null;
                } else {
                    participants += " " + sentence.get(i)[3];
                }

            }
            participants = participants.trim();
            //System.out.println("sentence is retrieved "+numEvents+"  "+Math.floor(sentence.size() / 10));
            //System.out.println(participants);


            for (Event e : events) {
                e.set_participants(participants);
            }
            // Fill only the particular participants for each event
        } else {
            for (Event e : events) {
                participants = fillParticipants(e.get_tok_num(), sentence, e.get_POS().substring(0, 1));
                e.set_participants(participants);
            }
        }

    }

    public static String fillParticipants(int position, ArrayList<String[]> sentence, String POS1) {

        //System.out.println(sentence.get(0)[1]+" "+sentence.get(0)[3]+" "+(sentence.size()-1)+" looking for "+position);
        String participants = "<b>" + sentence.get(position)[3] + "</b>";


        if (POS1.equalsIgnoreCase("V")) {
            participants = getVerbalParticipants(position, sentence, participants);
        } else {
            if (POS1.equalsIgnoreCase("N")) {
                participants = getNominalParticipants(position, sentence, participants);
            }
        }

        // add extra context (-3 event +3)
        if (participants.split(" ").length < 4) {
            participants = "<b>" + sentence.get(position)[3] + "</b>";
            int context = 3;
            int contextcount = 1;
            // left
            while (contextcount <= context && (position - contextcount) > 0) {
                participants = sentence.get(position - contextcount)[3] + " " + participants;
                contextcount++;
            }
            //right
            contextcount = 1;
            while (contextcount <= context && (position + contextcount) < sentence.size()) {
                participants = participants + " " + sentence.get(position + contextcount)[3];
                contextcount++;
            }
        }

        return participants;
    }

    public static String getVerbalParticipants(int position, ArrayList<String[]> sentence, String participant) {
        //get the A0,A1 for the current verb (if LOC show it but in a different way for the moment <i>
        // OTHER ROLES IGNORED IF THE A0 or A1 is a subsentence it may represent an SLINK not an event argument
        String participants = participant;
        String location = "", A2 = "", A3 = "", A4 = "";
        String srconfig = sentence.get(position)[11];
        String verb = sentence.get(position)[15];
        if (srconfig.contains("A0") || srconfig.contains("A1")) {
            participants = "";
            boolean hasA0 = false;
            boolean hasA1 = false;
            for (int i = 0; i < sentence.size(); i++) {
                if (i == position) {
                    participants += " <b>" + sentence.get(i)[3] + "</b>";
                } else {
                    if (sentence.get(i)[15].equalsIgnoreCase(verb)) {
                        if (sentence.get(i)[14].equalsIgnoreCase("A0")) {
                            hasA0 = true;
                            participants += " " + sentence.get(i)[3];
                        }
                        if (sentence.get(i)[14].equalsIgnoreCase("A1")) {
                            hasA1 = true;
                            participants += " " + sentence.get(i)[3];
                        }
                        if (sentence.get(i)[14].equalsIgnoreCase("AM-LOC") && sentence.get(i)[4].matches("NNP")) {
                            location += " " + sentence.get(i)[3];
                        }
                        if (sentence.get(i)[14].equalsIgnoreCase("A2")) {
                            A2 += " " + sentence.get(i)[3];
                        }
                        if (sentence.get(i)[14].equalsIgnoreCase("A3")) {
                            A3 += " " + sentence.get(i)[3];
                        }
                        if (sentence.get(i)[14].equalsIgnoreCase("A4")) {
                            A4 += " " + sentence.get(i)[3];
                        }
                    }
                }
            }

            participants = participants.trim();

            // complete verbs with possible lack of arguments
            if (!hasA0 || !hasA1) {
                if (!A2.equals("")) {
                    participants += A2;
                } else {
                    if (!A3.equals("")) {
                        participants += A3;
                    } else {
                        participants += A4;
                    }
                }
            }

            // indicate location if found
            if (!location.equals("")) {
                participants += " (LOC: " + location.trim() + ")";
            }
        }






        return participants;
    }

    public static String getNominalParticipants(int position, ArrayList<String[]> sentence, String participant) {
        // get the NP full

        String participants = participant;

        int currentSyntCount = parcount(sentence.get(position)[7], true);

        if (currentSyntCount == 0 && !(sentence.get(position)[7].length() > 1)) {
            // Search two directions
            // Backwards
            int searchposition = position - 1;
            while (currentSyntCount < 0 && searchposition == 0) {
                int par = parcount(sentence.get(searchposition)[7], false);
                participants = sentence.get(searchposition)[3] + " " + participants;
                searchposition--;
                if (par > 0) {
                    currentSyntCount = par;
                    break;
                }
            }
            // Forwards
            searchposition = position + 1;
            while (currentSyntCount > 0 && searchposition < sentence.size() && !sentence.get(searchposition)[7].contains("VP")) {
                participants += " " + sentence.get(searchposition)[3];
                currentSyntCount += parcount(sentence.get(searchposition)[7], false);
                searchposition++;
            }


        }




        // Search forwards
        if (currentSyntCount > 0) {
            int searchposition = position + 1;
            // Avoid verbs for rare noiman events NPs like (S (NP War) | (VP is) | (NP great))
            while (currentSyntCount > 0 && searchposition < sentence.size() && !sentence.get(searchposition)[7].contains("VP")) {
                participants += " " + sentence.get(searchposition)[3];
                currentSyntCount += parcount(sentence.get(searchposition)[7], false);
                searchposition++;
            }
        }

        // Search backwards just the current NP
        if (currentSyntCount < 0) {
            int searchposition = position - 1;
            //String lastphrase="";
            while (currentSyntCount < 0 && searchposition >= 0) { // && !sentence.get(searchposition)[7].contains("VP")) {
                int par = parcount(sentence.get(searchposition)[7], false);
                participants = sentence.get(searchposition)[3] + " " + participants;
                searchposition--;
                if (par > 0) {
                    break; // the first parenthesis will end the NP
                }
            }
        }

        return participants;
    }

    /**
     * Obtain the link training files from the key tml annotation and approach pipe features
     * @param tmlfile
     * @param featuresannotated
     */
    public static void get_features_from_pipes(PipesFile features_annotated, HashMap<String, ArrayList<String>> event_mk_index, HashMap<String, Timex> timexes, HashMap<String, Event> makeinstances) {
        int linen = 0;

        try {
            int iob2col = features_annotated.getColumn("element\\(IOB2\\)");
            int attrscol = iob2col + 1;
            int wordcolumn = features_annotated.getColumn("word");
            int syntcolumn = features_annotated.getColumn("synt");
            int phrasecol = features_annotated.getColumn("phra_id");
            int govPrepcol = features_annotated.getColumn("PPdetail");
            int rolecolumn = features_annotated.getColumn("simpleroles");
            Element inElem = null;
            SyntColParser sbarparser = null;
            SyntColSBarTMPRoleParser sbarroleparser = null;

            //String lastMainEvent = null;
            //String[] mainEvent = {"", "1000"}; // makeinstanceid and syntax level

            ArrayList<String[]> sentArray = null;


            BufferedReader pipesreader = new BufferedReader(new FileReader(features_annotated.getFile()));

            try {
                String pipesline;
                String[] pipesarr = null;
                String curr_docid = "";
                String curr_sentN = "";

                while ((pipesline = pipesreader.readLine()) != null) {
                    linen++;
                    pipesarr = pipesline.split("\\|");
                    //System.out.println(linen+"  "+pipesline);
                    if (!curr_docid.equals(pipesarr[0]) || !curr_sentN.equals(pipesarr[1])) {
                        // Add inElem elements before process
                        if (inElem != null && inElem.getClass().getSimpleName().equals("Timex")) {
                            timexes.put(inElem.get_id(), (Timex) inElem);
                        }
                        if (inElem != null && inElem.getClass().getSimpleName().equals("Event")) {
                            if (event_mk_index.containsKey(inElem.get_id())) {
                                ArrayList<String> event_mks = event_mk_index.get(inElem.get_id());
                                for (int i = 0; i < event_mks.size(); i++) {
                                    Event auxe = ((Event) inElem).clone();
                                    auxe.set_eiid(event_mks.get(i));
                                    makeinstances.put(event_mks.get(i), auxe);
                                }
                            }
                        }
                        inElem = null;
                        // initialize sentence
                        curr_sentN = pipesarr[1];
                        sentArray = null;
                        sentArray = new ArrayList<String[]>();
                        sbarparser = null;
                        sbarparser = new SyntColParser();
                        sbarroleparser = null;
                        sbarroleparser = new SyntColSBarTMPRoleParser();

                        //lastMainEvent = mainEvent[0]; // previous main event update
                        //mainEvent[0] = lastMainEvent; // That way if there is no event in the sentence it keeps the last one
                        //mainEvent[1] = "1000";

                        // initialize document
                        if (!curr_docid.equals(pipesarr[0])) {
                            // create new ones
                            curr_docid = pipesarr[0];
                            //lastMainEvent = null;
                        }

                    }

                    sentArray.add(pipesarr);

                    // Synt
                    boolean hasClosingBrakets = false;
                    if (pipesarr[syntcolumn].indexOf(')') != -1) {
                        hasClosingBrakets = true;
                    }
                    if (hasClosingBrakets) {
                        sbarparser.parse(pipesarr[syntcolumn].substring(0, pipesarr[syntcolumn].indexOf(')')));
                        sbarroleparser.parse(pipesarr[syntcolumn].substring(0, pipesarr[syntcolumn].indexOf(')')), pipesarr[rolecolumn], pipesarr[wordcolumn]);
                    } else {
                        sbarparser.parse(pipesarr[syntcolumn]);
                        sbarroleparser.parse(pipesarr[syntcolumn], pipesarr[rolecolumn], pipesarr[wordcolumn]);
                    }
                    if (pipesarr[iob2col].startsWith("B")) {
                        // Add current elem if exists
                        if (inElem != null) {
                            if (inElem != null && inElem.getClass().getSimpleName().equals("Timex")) {
                                timexes.put(inElem.get_id(), (Timex) inElem);
                            }
                            if (inElem.getClass().getSimpleName().equals("Event")) {
                                if (event_mk_index.containsKey(inElem.get_id())) {
                                    ArrayList<String> event_mks = event_mk_index.get(inElem.get_id());
                                    for (int i = 0; i < event_mks.size(); i++) {
                                        Event auxe = ((Event) inElem).clone();
                                        auxe.set_eiid(event_mks.get(i));
                                        makeinstances.put(event_mks.get(i), auxe);
                                    }
                                }
                            }
                            inElem = null;
                        }

                        // Start new element
                        HashMap<String, String> attrs = XmlAttribs.parseAttrs(pipesarr[attrscol]);
                        String element = pipesarr[iob2col].substring(2);
                        if (element.equalsIgnoreCase("event")) {
                            String id = attrs.get("eid");
                            String event_class = attrs.get("class");
                            inElem = new Event(id, pipesarr[3], event_class, curr_docid, Integer.parseInt(curr_sentN), Integer.parseInt(pipesarr[2].substring(0, pipesarr[2].indexOf('-'))));
                            ((Event) inElem).set_pos(Event.treebank2tml_pos(pipesarr[4]));
                            ((Event) inElem).set_tense_aspect_modality(pipesarr[features_annotated.getColumn("tense")],pipesarr[features_annotated.getColumn("pos")]);
                            ((Event) inElem).set_polarity(pipesarr[features_annotated.getColumn("assertype")]);
                        } else {
                            if (element.equalsIgnoreCase("timex") || element.equalsIgnoreCase("timex3")) {
                                String id = attrs.get("tid");
                                String type = attrs.get("type");
                                String value = attrs.get("value");
                                inElem = new Timex(id, pipesarr[wordcolumn], type, value, curr_docid, Integer.parseInt(curr_sentN), Integer.parseInt(pipesarr[2].substring(0, pipesarr[2].indexOf('-'))));
                            }
                        }
                        //subsent and phrase
                        if (inElem != null) {
                            //System.out.println(pipesarr[phrasecol]);
                            inElem.set_phra_id(pipesarr[phrasecol]);
                            inElem.set_subsent_num(sbarparser.getCurrentSubsent());
                            inElem.set_syntLevel(sbarparser.getParlevel());
                            inElem.set_govPrep(pipesarr[govPrepcol]);
                            inElem.set_govTMPSub(sbarroleparser.getSubsentTMP());
                            /*if (inElem.getClass().getSimpleName().equals("Event")) {
                            if (sbarparser.getParlevel() < Integer.parseInt(mainEvent[1]) || (sbarparser.getParlevel() == Integer.parseInt(mainEvent[1]) && makeinstances.get(mainEvent[0]).get_class().equalsIgnoreCase("ASPECTUAL") && !((Event) inElem).get_class().equalsIgnoreCase("ASPECTUAL"))) {
                            mainEvent[0] = inElem.get_id();
                            mainEvent[1] = "" + sbarparser.getParlevel();
                            }
                            }*/
                        }
                    }

                    // IMP: SIGNALS ARE OUTSIDE
                    if (pipesarr[iob2col].startsWith("I") && !pipesarr[iob2col].equalsIgnoreCase("I-SIGNAL")) {
                        if (inElem == null) {
                            System.err.println("Ignoring tag from KEY due to a bad sentence segmentation. Found I-X element without B-X. Line:" + linen);
                        } else {
                            inElem.extend_element(pipesarr[wordcolumn]);
                        }
                    }
                    if (pipesarr[iob2col].equals("O")) {
                        if (inElem != null && inElem.getClass().getSimpleName().equals("Timex")) {
                            timexes.put(inElem.get_id(), (Timex) inElem);
                        }
                        if (inElem != null && inElem.getClass().getSimpleName().equals("Event")) {
                            if (event_mk_index.containsKey(inElem.get_id())) {
                                ArrayList<String> event_mks = event_mk_index.get(inElem.get_id());
                                for (int i = 0; i < event_mks.size(); i++) {
                                    Event auxe = ((Event) inElem).clone();
                                    auxe.set_eiid(event_mks.get(i));
                                    makeinstances.put(event_mks.get(i), auxe);
                                }
                            }
                        }
                        inElem = null;
                    }

                    // Synt
                    if (hasClosingBrakets) {
                        sbarparser.parse(pipesarr[syntcolumn].substring(pipesarr[syntcolumn].indexOf(')')));
                        sbarroleparser.parse(pipesarr[syntcolumn].substring(pipesarr[syntcolumn].indexOf(')')), pipesarr[rolecolumn], pipesarr[wordcolumn]);
                    }


                }

                if (inElem != null) {
                    if (inElem != null && inElem.getClass().getSimpleName().equals("Timex")) {
                        timexes.put(inElem.get_id(), (Timex) inElem);
                    }
                    if (inElem.getClass().getSimpleName().equals("Event")) {
                        if (event_mk_index.containsKey(inElem.get_id())) {
                            ArrayList<String> event_mks = event_mk_index.get(inElem.get_id());
                            for (int i = 0; i < event_mks.size(); i++) {
                                Event auxe = ((Event) inElem).clone();
                                auxe.set_eiid(event_mks.get(i));
                                makeinstances.put(event_mks.get(i), auxe);
                            }
                        }
                    }
                    inElem = null;
                }

                /*// set main event
                if (!mainEvent[0].equals("")) {
                makeinstances.get(mainEvent[0]).set_is_main(true);
                }*/
            } finally {
                if (pipesreader != null) {
                    pipesreader.close();
                }

            }

        } catch (Exception e) {
            System.err.println("Errors found (TempEval):\n\t" + e.toString() + " (Reading line " + linen + " " + features_annotated.getFile() + ")\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

    public static int parcount(String synt, boolean discardPP) {
        int count = 0;
        boolean P_in_row = false; // for discard PPs started by nouns: Days before his conference, he... --> Noun Days starts a PP
        for (int i = 0; i < synt.length(); i++) {
            if (synt.charAt(i) == '(') {
                count++;
            } else {
                if (synt.charAt(i) == ')') {
                    count--;
                }
            }

            // discard PPs started by nouns
            if (discardPP) {
                if (synt.charAt(i) == 'P') {
                    if (P_in_row) {
                        count--;
                        P_in_row = false;
                    } else {
                        P_in_row = true;
                    }
                } else {
                    P_in_row = false;
                }
            }

        }
        return count;
    }

    public static void get_mk_and_links_from_tab_event_timex(String tab, HashMap<String, HashMap<String, Event>> makeinstances, HashMap<String, HashMap<String, Link>> links) {
        int linen = 0;
        try {
            BufferedReader pipesreader = new BufferedReader(new FileReader(new File(tab)));
            HashMap<String, Event> doc_makeinstances = null;
            HashMap<String, Link> doc_links = null;

            try {
                String tabline;
                String[] tabsarr = null;
                String curr_docid = "";

                while ((tabline = pipesreader.readLine()) != null) {
                    linen++;
                    tabsarr = tabline.split("\t");

                    // For avoid corpus empty values
                    if(tabsarr.length!=5){
                        System.err.println("Link without relType in line "+linen);
                        continue;
                    }
                    // For avoid corpus vague/none values
                    if(tabsarr[4].matches("(?i)(?:VAGUE|NONE|UNKNOWN)")){
                        System.err.println("Vague/none/unknown Link ignored in line "+linen);
                        continue;
                    }


                    //System.out.println(linen+"  "+tabline);
                    if (!curr_docid.equals(tabsarr[0])) {
                        // save previous
                        if (!curr_docid.equals("")) {
                            makeinstances.put(curr_docid, doc_makeinstances);
                            links.put(curr_docid, doc_links);
                        }
                        // create new ones
                        curr_docid = tabsarr[0];
                        doc_makeinstances = null;
                        doc_links = null;

                        if (!makeinstances.containsKey(curr_docid)) {
                            doc_makeinstances = new HashMap<String, Event>();
                            doc_links = new HashMap<String, Link>();
                        } else {
                            doc_makeinstances = makeinstances.get(curr_docid);
                            doc_links = links.get(curr_docid);
                        }
                    }

                  // docid eid eiid tid reltype
                  String eiid=tabsarr[1].replace("e", "ei");
                  if(!tabsarr[2].equals("ei1")){
                        int tempid = Integer.parseInt(eiid.substring(2));
                        String newid = "ei" + Event.firstExtraMakeinstanceId.substring(0, Event.firstExtraMakeinstanceId.length() - ("" + tempid).length()) + tempid;
                        while (doc_makeinstances.containsKey(newid)) {
                            newid = "ei" + Event.firstExtraMakeinstanceId.substring(0, Event.firstExtraMakeinstanceId.length() - ("" + tempid).length()) + tempid;
                            tempid++;
                        }
                        eiid=newid;
                  }
                  if(!doc_makeinstances.containsKey(eiid)){
                      Event auxe=new Event(tabsarr[1], "-", "-", curr_docid, -1, -1);
                      auxe.set_eiid(eiid);
                      doc_makeinstances.put(eiid, auxe);
                  }
                  doc_links.put("l"+(doc_links.size()+1), new Link("l"+(doc_links.size()+1), "tlink-event-timex", tabsarr[4], eiid, tabsarr[3], curr_docid));
                }
                makeinstances.put(curr_docid, doc_makeinstances);
                links.put(curr_docid, doc_links);

            } finally {
                if (pipesreader != null) {
                    pipesreader.close();
                }
            }

        } catch (Exception e) {
            System.err.println("Errors found (TempEval):\n\t" + e.toString() + " (Reading line " + linen + " " + tab + ")\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }


    public static void get_mk_and_links_from_tab_event_event(String tab, HashMap<String, HashMap<String, Event>> makeinstances, HashMap<String, HashMap<String, Link>> links) {
        int linen = 0;
        try {
            BufferedReader pipesreader = new BufferedReader(new FileReader(new File(tab)));
            HashMap<String, Event> doc_makeinstances = null;
            HashMap<String, Link> doc_links = null;

            try {
                String tabline;
                String[] tabsarr = null;
                String curr_docid = "";

                while ((tabline = pipesreader.readLine()) != null) {
                    linen++;
                    tabsarr = tabline.split("\t");
                    //System.out.println(linen+"  "+tabline);
                    if (!curr_docid.equals(tabsarr[0])) {
                        // save previous
                        if (!curr_docid.equals("")) {
                            makeinstances.put(curr_docid, doc_makeinstances);
                            links.put(curr_docid, doc_links);
                        }
                        // create new ones
                        curr_docid = tabsarr[0];
                        doc_makeinstances = null;
                        doc_links = null;

                        if (!makeinstances.containsKey(curr_docid)) {
                            doc_makeinstances = new HashMap<String, Event>();
                            doc_links = new HashMap<String, Link>();
                        } else {
                            doc_makeinstances = makeinstances.get(curr_docid);
                            doc_links = links.get(curr_docid);
                        }
                    }




                }
                makeinstances.put(curr_docid, doc_makeinstances);
                links.put(curr_docid, doc_links);

            } finally {
                if (pipesreader != null) {
                    pipesreader.close();
                }
            }

        } catch (Exception e) {
            System.err.println("Errors found (TempEval):\n\t" + e.toString() + " (Reading line " + linen + " " + tab + ")\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

}
