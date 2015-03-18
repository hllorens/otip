package com.cognitionis.tipsem;

import com.cognitionis.timeml_basickit.Link;
import com.cognitionis.timeml_basickit.Timex;
import com.cognitionis.timeml_basickit.TimeReference;
import com.cognitionis.timeml_basickit.Event;
import com.cognitionis.timeml_basickit.comparators.AscStringTimeRefMapComparator;
import com.cognitionis.knowledgek.TIMEK.TIMEK;
import java.io.*;
import java.util.*;
import com.cognitionis.nlp_files.PipesFile;
import org.joda.time.DateTime;

/**
 * @author Hector Llorens
 * @since 2011
 */
public class TAn {

    public static String fake_TAn(HashMap<String, Timex> timexes, HashMap<String, Event> events, HashMap<String,Link> links, String o, String dct, String ommit_current_implicit) {
        String outputfile = null;

        //Timex earliestRef=null,latestRef=null;

        HashMap<String, TimeReference> refs = new HashMap<String, TimeReference>();
        Integer TimeRefsId = 1;

        try {
            outputfile = o + ".TAn";
            BufferedWriter outfile = new BufferedWriter(new FileWriter(outputfile));

            try {
                boolean ommitCurrent = false;
                String current_date = new DateTime().toString("YYYY-MM-dd");
                int current_year = Integer.parseInt(current_date.substring(0, 4));

                if (ommit_current_implicit == null) {
                    Date earliest = null;
                    for (String current_key : timexes.keySet()) {
                        Timex t = timexes.get(current_key);
                        if (t.get_value().matches("(?i)[0-9]{4}.*")) {
                            //System.out.println(t.get_expression() +"  "+t.get_date().toString());
                            if (earliest == null) {
                                earliest = t.get_date();
                                // System.out.println("--->"+earliest.toString().substring((earliest.toString().length())-5)+" "+t.get_value());
                            } else {
                                if (t.get_date() != null && t.get_date().before(earliest)) {
                                    earliest = t.get_date();
                                  //  System.out.println("--->"+earliest.toString().substring((earliest.toString().length())-5)+" "+t.get_value());
                                }
                            }
                        }
                    }
                    //System.out.println("--->"+earliest.toString().substring((earliest.toString().length())-5));
                    if (dct.equals(current_date) && earliest != null && (current_year - Integer.parseInt(earliest.toString().substring(earliest.toString().lastIndexOf("ET ")+3,earliest.toString().length()))) > 20) {
                        System.out.println("Ommiting current dates");
                        ommitCurrent = true;
                    }
                } else {
                    if (ommit_current_implicit.equalsIgnoreCase("true")) {
                        ommitCurrent = true;
                    }
                }

                outfile.write("{\n\t\"label\": \"" + o.substring(o.lastIndexOf('/') + 1, o.lastIndexOf('.')).replaceAll("\"", "\\\"") + "\",\n\t\"data\": [\n");
                for (Link l : links.values()) {
                    if (l.get_type().equalsIgnoreCase("tlink-event-timex")) {
                        if (!ommitCurrent
                                || (ommitCurrent && Math.abs(current_year - Integer.parseInt(timexes.get(l.get_id2()).get_value().substring(0, 4))) > 1)
                                || timexes.get(l.get_id2()).get_expression().matches("(.* )?[0-9]{4}( .*)?")) {

                            /*if (timexes.get(l.get_id2()).get_value().substring(0, 4).equals("2010")) {
                            System.out.println(current_year + " ||| " + timexes.get(l.get_id2()).get_expression());
                            }*/
                            //System.out.println(l.get_id()+" "+l.get_id1()+" "+l.get_id2()+" "+timexes.get(l.get_id2()).get_expression());
                            String datekey = timexes.get(l.get_id2()).get_date().toString();
                            //System.out.println(datekey);
                            if (refs.containsKey(datekey)) {
                                //DEPRECATED ((TimeReference) refs.get(String.valueOf(datekey))).add_event(events.get("e"+(Integer.parseInt(l.get_id1().substring(1)) - 1)));
                                ((TimeReference) refs.get(String.valueOf(datekey))).add_event(events.get(l.get_id1()));
                            } else {
                                // DEPRECATED refs.put(String.valueOf(datekey), new TimeReference(TimeRefsId.toString(), timexes.get(l.get_id2()), events.get("e"+(Integer.parseInt(l.get_id1().substring(1)) - 1))));
                                refs.put(String.valueOf(datekey), new TimeReference(TimeRefsId.toString(), timexes.get(l.get_id2()), events.get(l.get_id1())));
                                TimeRefsId++;
                            }
                        }
                    }
                    // TODO: ver como incluir el resto de links (TENER EN CUENTA EL OMMIT CURRENT)
                }

                // check if all the refs contain valid dates
                for (String key : refs.keySet()) {
                    TimeReference tr = refs.get(key);
                    if (tr.get_timex().get_date() == null) {
                        System.out.println("PROBLEMA: " + key + ". " + tr.get_id() + ". " + tr.get_timex().get_id() + ". " + tr.get_timex().get_value() + ": " + tr.get_events().size() + " events\n");
                        System.exit(0);
                    }
                }

                TreeMap<String, TimeReference> sorted_refs = new TreeMap(new AscStringTimeRefMapComparator(refs));
                sorted_refs.putAll(refs);

                int countgroups = 0;
                for (String key : sorted_refs.keySet()) {
                    TimeReference tr = sorted_refs.get(key);

                    countgroups++;
                    if (countgroups > 1) {
                        outfile.write(",\n");
                    }
                    String eventss = "[";
                    int ecount = 0;
                    for (Event e : tr.get_events()) {
                        if (e != null) {
                            ecount++;
                            if (ecount > 1) {
                                eventss += ",";
                            }
                            eventss += "[\"" + e.get_id() + "\",\"" + e.get_expression().replaceAll("\"", "\\\\\"") + "\",\"" + e.get_participants().replaceAll("\"", "\\\\\"") + "\",\"" + e.get_class() + "\",\"" + e.get_POS() + "\",\"" + TIMEK.add_duration_2_timeref(tr.get_timex().get_date(), e.get_duration()).getTime() + "\",\"" + e.get_aspectual_modifier() + "\",\"" + e.get_doc_id() + "\",\"sent" + e.get_sent_num() + "\"]";
                        }
                    }
                    eventss += "]";
                    outfile.write("[" + tr.get_timex().get_date().getTime() + ",1," + tr.get_events().size() + ",\"" + tr.get_timex().get_value() + "\"," + eventss + "]");
                }

                outfile.write("\n]\n}\n");
            } finally {
                if (outfile != null) {
                    outfile.close();
                }
            }



        } catch (Exception e) {
            System.err.println("Errors found (TempEval):\n\t" + e.toString() + "\n");
            if (System.getProperty("DEBUG") != null && System.getProperty("DEBUG").equalsIgnoreCase("true")) {
                e.printStackTrace(System.err);
                System.exit(1);
            }
            return null;
        }
        return outputfile;
    }

    public static String pipes2navphp(PipesFile pipesfile) {
        String outputfile = null;
        int linen = 0;

        try {
            outputfile = pipesfile.getFile().getCanonicalPath() + ".nav.php";
            BufferedWriter outfile = new BufferedWriter(new FileWriter(outputfile));
            BufferedReader pipesreader = new BufferedReader(new FileReader(pipesfile.getFile()));
            try {
                String pipesline;
                String[] pipesarr = null;

                String curr_fileid = "";
                String curr_sentN = "";


                outfile.write("<?php header('Content-type: text/html; charset=utf-8'); ?><html><head><meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" /><title>Navigable HTML document</title></head><body>");


                while ((pipesline = pipesreader.readLine()) != null) {
                    linen++;
                    pipesarr = pipesline.split("\\|");
                    if (!curr_fileid.equals(pipesarr[0])) {
                        if (!curr_fileid.equals("")) {
                            outfile.write("\n</div>\n\n");

                        }
                        curr_fileid = pipesarr[0];

                        outfile.write("\n<h1>" + curr_fileid + "</h1>\n");
                        outfile.write("\n<div>\n");
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
                                    preceding_blanks += "<br />";
                                }
                            }
                        }
                    }
                    outfile.write(preceding_blanks);

                    if (!curr_sentN.equals(pipesarr[1])) {
                        if (!curr_sentN.equals("")) {
                            outfile.write("</span>\n");
                        }
                        curr_sentN = pipesarr[1];
                        outfile.write("\n\t<span id=\"sent" + curr_sentN + "\">");
                    }

                    outfile.write(pipesarr[3]);
                }

                outfile.write("\n<br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br /><br />Time-Surfer Source Text<br /><br /><br /><br /><br /><br /></body>\n");
                outfile.write("\n</html>\n");

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
}
