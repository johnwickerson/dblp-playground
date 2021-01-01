//
// Copyright (c)2015, dblp Team (University of Trier and Schloss
// Dagstuhl - Leibniz-Zentrum fuer Informatik GmbH) All rights
// reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//
// (1) Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//
// (2) Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following
// disclaimer in the documentation and/or other materials provided
// with the distribution.
//
// (3) Neither the name of the dblp team nor the names of its
// contributors may be used to endorse or promote products derived
// from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL DBLP
// TEAM BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
// OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
// USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
// DAMAGE.
//

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.dblp.DblpInterface;
import org.dblp.mmdb.Field;
import org.dblp.mmdb.FieldReader;
import org.dblp.mmdb.Person;
import org.dblp.mmdb.PersonName;
import org.dblp.mmdb.Publication;
import org.dblp.mmdb.datastructure.SimpleLazyCoauthorGraph;
import org.xml.sax.SAXException;


@SuppressWarnings("javadoc")
class DblpExampleParser {

  static String getTimestamp() {
    return new SimpleDateFormat("HH:mm:ss").format(new Date());
  }
  
  static DblpInterface mkInterface(String dblpXmlFilename) {
    System.out.format("%s, building the dblp main memory DB ...\n",
		      getTimestamp());
    DblpInterface dblp;
    try {
      dblp = new SimpleLazyCoauthorGraph(dblpXmlFilename);
    }
    catch (final IOException ex) {
      System.err.println("cannot read dblp XML: " + ex.getMessage());
      System.exit(1);
      return null;
    }
    catch (final SAXException ex) {
      System.err.println("cannot parse XML: " + ex.getMessage());
      System.exit(1);
      return null;
    }
    System.out.format("%s, MMDB ready: %d publs, %d pers\n\n",
		      getTimestamp(), dblp.numberOfPublications(),
		      dblp.numberOfPersons());
    return dblp;
  }

  static Comparator<Person> person_cmp =
      (Person o1, Person o2) ->
      o1.getPrimaryName().getName().
      compareTo(o2.getPrimaryName().getName());

  static boolean paperTooShort(String str_pages) {
    if (str_pages == null) return true; // no "pages" field ==> not proper.
    String[] arr_pageranges = str_pages.split(",");
    str_pages = arr_pageranges[0]; // occasionally papers have multiple ranges!
    String[] arr_pages = str_pages.split("-");
    if (arr_pages.length == 1) return true; // single page ==> not proper.
    String str_start = arr_pages[0];
    String[] arr_start = str_start.split(":");
    str_start = arr_start[arr_start.length - 1];
    int start = Integer.parseInt(str_start);
    String str_end = arr_pages[1];
    String[] arr_end = str_end.split(":");
    str_end = arr_end[arr_end.length - 1];
    int end = Integer.parseInt(str_end);
    int paper_length = end - start + 1;
    return (paper_length < 4); // proper papers are at least 4 pages.

  }

  static Map<Person, List<Integer>>
    findAuthorsByYear(DblpInterface dblp, List<String> confName) {
    return findAuthorsByYear(dblp, null, confName, 1900, 2500);
  }
  
  static Map<Person, List<Integer>>
    findAuthorsByYear(DblpInterface dblp, String[] authors_of_interest, List<String> confName, int first_year, int last_year) {
    Map<Person, List<Integer>> years_of = new TreeMap(person_cmp);
    
    for (Publication publ : dblp.getPublications()) {
      FieldReader reader = publ.getFieldReader();
      
      if (confName.contains(reader.valueOf("booktitle")) ||
	  confName.contains(reader.valueOf("number"))) {
	if (paperTooShort(reader.valueOf("pages"))) continue;
	int year = Integer.parseInt(reader.valueOf("year"));
	if (year < first_year) continue;
	if (year > last_year) continue;
	for (PersonName name : publ.getNames()) {
	  Person pers = name.getPerson();
	  if (authors_of_interest != null) {
	    boolean of_interest = false;
	    for (String name_of_interest : authors_of_interest)
	      if (name_of_interest.equals(pers.getPrimaryName().getName()))
		of_interest = true;
	    if (! of_interest) continue;
	  }
	  if (! years_of.containsKey(pers)) {
	    years_of.put(pers, new ArrayList());
	  }
	  years_of.get(pers).add(year);
	}
      }
    }
    
    for (Person p : years_of.keySet()) {
      List<Integer> ys = years_of.get(p);
      Collections.sort(ys);
    }
    return years_of;
  }

  static List<String>
    findTitles(DblpInterface dblp, List<String> confName, String[] authors_of_interest, int first_year, int last_year) {
    List<String> titles = new ArrayList();
    
    for (Publication publ : dblp.getPublications()) {
      FieldReader reader = publ.getFieldReader();
      
      if (confName.contains(reader.valueOf("booktitle")) ||
	  confName.contains(reader.valueOf("number"))) {
	if (paperTooShort(reader.valueOf("pages"))) continue;
	int year = Integer.parseInt(reader.valueOf("year"));
	if (year < first_year) continue;
	if (year > last_year) continue;
	if (authors_of_interest != null) {
	  boolean of_interest = false;
	  for (PersonName name : publ.getNames()) {
	    Person pers = name.getPerson();
	    for (String name_of_interest : authors_of_interest)
	      if (name_of_interest.equals(pers.getPrimaryName().getName())) {
		of_interest = true;
		System.out.format("Found %s paper from %d by %s.\n", confName.get(0), year, name_of_interest);
		break;
	      }
	  }
	  if (! of_interest) continue;
	}
	
	titles.add(reader.valueOf("title"));
      }
    }

    Collections.sort(titles);

    //System.out.format("First alphabetically: %s\n", titles.get(0));
    //for (int i = 1; i < 50; i++) 
    //  System.out.format("Last alphabetically: %s\n", titles.get(titles.size() - i));

    Comparator<String> compare_by_length =
      (String s1, String s2) -> {
      return Integer.compare(s1.length(), s2.length());
    };
    
    titles.sort(compare_by_length);

    System.out.format("Number of titles: %d.\n", titles.size());
    
    return titles;
  }

  static void
    findAuthorLists(DblpInterface dblp, List<String> confName) {
    List<Publication> pubs = new ArrayList();
    
    for (Publication publ : dblp.getPublications()) {
      FieldReader reader = publ.getFieldReader();
      
      if (confName.contains(reader.valueOf("booktitle")) ||
	  confName.contains(reader.valueOf("number"))) {
	if (paperTooShort(reader.valueOf("pages"))) continue;
	pubs.add(publ);
      }
    }

    Comparator<Publication> compare_by_authorship =
      (Publication p1, Publication p2) -> {
      return Integer.compare(p1.getNames().size(), p2.getNames().size());
    };
    
    pubs.sort(compare_by_authorship);

    int single_authors = 0;
    for (int i = 0; i < 250; i++) {
      Publication publ =  pubs.get(i);
      if (publ.getNames().size() == 1)
	single_authors++;
      else break;
    }
    System.out.format("1 author: %d publications\n", single_authors);
    
    for (int i = 1; i < 50; i++) {
      Publication publ =  pubs.get(pubs.size() - i);
      FieldReader reader = publ.getFieldReader();
      System.out.format("%d author(s): %s\n", publ.getNames().size(), reader.valueOf("title"));
    }
   
  }

  static void
    findShortestTitles(List<String> titles) {

    int targetLen = titles.get(0).length();
    for (int i = 0; i < titles.size(); i++) {
      String title = titles.get(i);
      if (title.length() == targetLen)
	System.out.format("%s\n", title);
      else break;
    }
  }

  static void
    findLongestTitles(List<String> titles) {

    int targetLen = titles.get(titles.size() - 1).length();
    for (int i = titles.size() - 1; i >= 0; i--) {
      String title = titles.get(i);
      if (title.length() == targetLen)
	System.out.format("%s\n", title);
      else break;
    }
  }
  
  static void printList (Map<Person, List<Integer>> years_of) {
    
    // Print each author together with their list of years
    for (Person p : years_of.keySet()) {
      String url = p.getKey().replace("homepages", "http://dblp.org/pid");
      System.out.format("%s [%s]:", p.getPrimaryName().getName(), url);
      for (int y : years_of.get(p)) {
	System.out.format(" %d", y);
      }
      System.out.format("\n");
      /*for (String k : p.getAttributes().keySet()) {
	String v = p.getAttributes().get(k);
	System.out.format(" %s=%s", k, v);
      }*/
    }
    System.out.format("\n");
  }

  static void findMostPapers(Map<Person, List<Integer>> years_of,
			     int max_pos) {
    // Find authors with most papers
    System.out.format("%s, finding authors with most papers...\n",
		      getTimestamp());
    List<Person> ps = new ArrayList(years_of.keySet());
    Comparator<Person> compare_by_num_papers =
      (Person p1, Person p2) -> {
      int count1 = years_of.get(p1).size();
      int count2 = years_of.get(p2).size();
      return Integer.compare(count2, count1);
    };
    ps.sort(compare_by_num_papers);
    int i = 0;
    for (int pos = 0; pos < max_pos; pos++) {
      System.out.format("<li>%d. ", pos + 1);
      while (i < ps.size()) {
	Person p = ps.get(i);
	int count = years_of.get(p).size();
	String name = p.getPrimaryName().getName();
	String url = p.getKey().replace("homepages", "http://dblp.org/pid");
	System.out.format("<a href=\"%s\">%s</a>", url, name);
	i++;
	if (years_of.get(ps.get(i)).size() != count) {
	  System.out.format(" (%d papers)</li>\n", count);
	  break;
	}
	System.out.format(", ");
      }
    }
    System.out.format("\n");
  }

  static void findMostConfs(Map<Person, List<Integer>> years_of,
			    int max_pos) {
    // Find authors with papers at the most conferences
    System.out.format("%s, finding authors with papers at the most conferences ...\n",
		      getTimestamp());
    List<Person> ps = new ArrayList(years_of.keySet());
    Comparator<Person> compare_by_num_confs =
      (Person p1, Person p2) -> {
      int count1 = new HashSet(years_of.get(p1)).size();
      int count2 = new HashSet(years_of.get(p2)).size();
      return Integer.compare(count2, count1);
    };
    ps.sort(compare_by_num_confs);
    int i = 0;
    for (int pos = 0; pos < max_pos; pos++) {
      System.out.format("<li>%d. ", pos + 1);
      while (i < ps.size()) {
	Person p = ps.get(i);
	int count = new HashSet(years_of.get(p)).size();
	String name = p.getPrimaryName().getName();
	String url = p.getKey().replace("homepages", "http://dblp.org/pid");
	System.out.format("<a href=\"%s\">%s</a>", url, name);
	i++;
	if (new HashSet(years_of.get(ps.get(i))).size() != count) {
	  System.out.format(" (%d conferences)</li>\n", count);
	  break;
	}
	System.out.format(", ");
      }
    }
    System.out.format("\n");
  }

  static void findLongestVacation(Map<Person, List<Integer>> years_of,
			     int max_pos) {
    // Find authors with the longest gap
    System.out.format("%s, finding authors with the longest holiday...\n", getTimestamp());
    Map<Person, Integer> gap_of = new TreeMap(person_cmp);
    Map<Person, Integer> gap_start_year_of = new TreeMap(person_cmp);
    for (Person p : years_of.keySet()) {
      List<Integer> years = years_of.get(p);
      Collections.sort(years);
      int prev_year = years.get(0);
      int gap = 0;
      int gap_start_year = 0;
      for (int cur_year : years) {
	if (cur_year - prev_year > gap) {
	  gap = cur_year - prev_year;
	  gap_start_year = prev_year;
	}
	prev_year = cur_year;
      }
      gap_of.put(p,gap);
      gap_start_year_of.put(p,gap_start_year);
    }
    List<Person> ps = new ArrayList(years_of.keySet());
    Comparator<Person> compare_by_gap =
      (Person p1, Person p2) -> {
      return Integer.compare(gap_of.get(p2), gap_of.get(p1));
    };
    ps.sort(compare_by_gap);
    int pos = 0;
    int prev_gap = 0;
    for (Person p : ps) {
      int gap = gap_of.get(p);
      int gap_start_year = gap_start_year_of.get(p);
      if (prev_gap != gap) pos++;
      prev_gap = gap;
      if (pos > max_pos && max_pos != 0) break;
      String name = p.getPrimaryName().getName();
      String url = p.getKey().replace("homepages", "http://dblp.org/pid");
      System.out.format("<li>%d. <a href=\"%s\">%s</a> (%d to %d: %d-year gap)</li>\n",
			pos, url, name, gap_start_year, gap_start_year + gap, gap);
      // for (int y : years_of.get(p)) {
      //   System.out.format(" %d", y);
      // }
      // System.out.format("\n");
    }
    System.out.format("\n");
  }

  static void findLongestCareer(Map<Person, List<Integer>> years_of,
			       int max_pos) {
    // Find authors with the longest range
    System.out.format("%s, finding authors with the longest range...\n", getTimestamp());
    Map<Person, Integer> range_of = new TreeMap(person_cmp);
    Map<Person, Integer> range_start_of = new TreeMap(person_cmp);
    for (Person p : years_of.keySet()) {
      List<Integer> years = years_of.get(p);
      Collections.sort(years);
      int first_year = years.get(0);
      int last_year = years.get(years.size() - 1);
      range_of.put(p, last_year - first_year);
      range_start_of.put(p, first_year);
    }
    List<Person> ps = new ArrayList(years_of.keySet());
    Comparator<Person> compare_by_range =
      (Person p1, Person p2) -> {
      return Integer.compare(range_of.get(p2), range_of.get(p1));
    };
    ps.sort(compare_by_range);
    int pos = 0;
    int prev_range = 0;
    for (Person p : ps) {
      int range = range_of.get(p);
      int range_start = range_start_of.get(p);
      if (prev_range != range) pos++;
      prev_range = range;
      if (pos > max_pos && max_pos != 0) break;
      String name = p.getPrimaryName().getName();
      String url = p.getKey().replace("homepages", "http://dblp.org/pid");
      System.out.format("<li>%d. <a href=\"%s\">%s</a> (%d to %d: %d years)</li>\n",
			pos, url, name, range_start, range_start + range, range);
      /*
      for (int y : years_of.get(p)) {
	System.out.format(" %d", y);
      }
      System.out.format("\n");
      */
    }
    System.out.format("\n");
  }
  
  static void findLongestStreak(Map<Person, List<Integer>> years_of,
				int max_pos, int this_year) {
    // Find authors with the longest streak
    System.out.format("%s, finding authors with the longest streak...\n", getTimestamp());
    Map<Person, Integer> streak_of = new TreeMap(person_cmp);
    Map<Person, Integer> streak_end_of = new TreeMap(person_cmp);
    for (Person p : years_of.keySet()) {
      List<Integer> years = years_of.get(p);
      Set<Integer> year_set = new HashSet(years);
      years = new ArrayList(year_set);
      Collections.sort(years);
      int longest_streak = 1;
      int streak_end = 0;
      int current_streak = 1;
      for (int y : years) {
	if (years.contains(y + 1)) {
	  current_streak = current_streak + 1;
	} else {
	  if (current_streak >= longest_streak) {
	    longest_streak = current_streak;
	    streak_end = y;
	  }
	  current_streak = 1;
	}
      }
      streak_of.put(p,longest_streak);
      streak_end_of.put(p, streak_end);
    }
    List<Person> ps = new ArrayList(years_of.keySet());
    Comparator<Person> compare_by_streak =
      (Person p1, Person p2) -> {
      return Integer.compare(streak_of.get(p2), streak_of.get(p1));
    };
    ps.sort(compare_by_streak);
    int pos = 0;
    int prev_streak = 0;
    for (Person p : ps) {
      int streak = streak_of.get(p);
      int streak_end = streak_end_of.get(p);
      if (prev_streak != streak) pos++;
      prev_streak = streak;
      if (pos > max_pos && max_pos != 0) break;
      String name = p.getPrimaryName().getName();
      String url = p.getKey().replace("homepages", "http://dblp.org/pid");
      if (streak_end == this_year) {
	System.out.format("<li>%d. <a href=\"%s\">%s</a> (since %d: %d conferences)</li>\n",
			pos, url, name, streak_end - streak + 1, streak);
      } else {
	System.out.format("<li>%d. <a href=\"%s\">%s</a> (%d to %d: %d conferences)</li>\n",
			pos, url, name, streak_end - streak + 1, streak_end, streak);
	
      }
      /* 
      for (int y : years_of.get(p)) {
	System.out.format(" %d", y);
      }
      System.out.format("\n");
      */
    }
    System.out.format("\n");
  }

  static void findMostPapersPerConf(Map<Person, List<Integer>> years_of,
				    int max_pos) {
    // Find authors with the most papers per conference
    System.out.format("%s, finding authors with the most papers per conference...\n", getTimestamp());
    Map<Person, Integer> ppc_of = new TreeMap(person_cmp);
    Map<Person, List<Integer>> best_confs_of = new TreeMap(person_cmp);
    for (Person p : years_of.keySet()) {
      List<Integer> years = years_of.get(p);
      List<Integer> best_confs = new ArrayList();
      Collections.sort(years);
      int max_ppc = 1;
      int cur_ppc = 1;
      int prev_y = 0;
      int cur_y = 0;
      for (int i = 0; i < years.size(); i++) {
	cur_y = years.get(i);
	if (prev_y == cur_y)
	  cur_ppc = cur_ppc + 1;
        else
	  cur_ppc = 1;
	if (cur_ppc == max_ppc) {
	  best_confs.add(cur_y);
	} else if (cur_ppc > max_ppc) {
	  max_ppc = cur_ppc;
	  best_confs.clear();
	  best_confs.add(cur_y);
	}
	prev_y = cur_y;
      }
      ppc_of.put(p, max_ppc);
      best_confs_of.put(p, best_confs);
    }
    List<Person> ps = new ArrayList(years_of.keySet());
    Comparator<Person> compare_by_ppc =
      (Person p1, Person p2) -> {
      int i = Integer.compare(ppc_of.get(p2), ppc_of.get(p1));
      if (i == 0) {
	return Integer.compare(best_confs_of.get(p2).size(), best_confs_of.get(p1).size());
      } else return i;
    };
    ps.sort(compare_by_ppc);
    int pos = 0;
    int prev_ppc = 0;
    for (Person p : ps) {
      int ppc = ppc_of.get(p);
      List<Integer> best_confs = best_confs_of.get(p);
      if (prev_ppc != ppc) pos++;
      prev_ppc = ppc;
      if (pos > max_pos && max_pos != 0) break;
      String name = p.getPrimaryName().getName();
      String url = p.getKey().replace("homepages", "http://dblp.org/pid");
      System.out.format("<li>%d. <a href=\"%s\">%s</a> (%d papers in ",
			pos, url, name, ppc);
      boolean first = true;
      for (int y : best_confs) {
	if (first) System.out.format("%d", y);
	else System.out.format(", %d", y);
	first = false;
      }
      System.out.format(")</li>\n");
      /*
      for (int y : years_of.get(p)) {
	System.out.format(" %d", y);
      }
      System.out.format("\n");
      */
    }
    System.out.format("\n");
  }
  
  public static void main(String[] args) {

    List<String> conf = new ArrayList();
    //conf.add("FPT"); conf.add("ICFPT");
    //conf.add("FPGA");
    //conf.add("FCCM");
    //conf.add("FPL");
    //conf.add("PLDI");
    //conf.add("ICFP");
    //conf.add("OOPSLA");
    conf.add("POPL");
    int this_year = 2020;
    
    // we need to raise entityExpansionLimit because the dblp.xml has millions of entities
    System.setProperty("entityExpansionLimit", "10000000");
    
    if (args.length != 1) {
      System.err.format("Usage: java %s <dblp-xml-file>\n", DblpExampleParser.class.getName());
      System.exit(0);
    }
    String dblpXmlFilename = args[0];
    
    DblpInterface dblp = mkInterface(dblpXmlFilename);

    /*String[] authors_of_interest =
  {"Isil Dillig",
   "Nir Piterman",
   "Cesare Tinelli",
   "Nikolaj Bjørner",
   "Arie Gurfinkel",
   "Ufuk Topcu",
   "Wei-Ngan Chin",
   "Alan J. Hu",
   "Franjo Ivaňcíc",
   "Grigore Roşu",
   "Margus Veanes",
   "Christel Baier",
   "Natasha Sharygina",
   "Aarti Gupta",
   "Eran Yahav",
   "Rajeev Alur",
   "Patrice Godefroid",
   "Koushik Sen",
   "Gilles Barthe",
   "Chris Hawblitzel",
   "Joao Marques-Silva",
   "Mayur Naik",
   "Roderick Bloem",
   "Adam Chlipala",
   "Ranjit Jhala",
   "Natarajan Shankar",
   "Hongseok Yang",
   "Sanjit A. Seshia",
   "Klaus Havelund"};*/
    

    /*String[] authors_of_interest =
  {"Walter Binder",
   "Martin Hirzel",
   "Suresh Jagannathan",
   "Gabriele Keller",
   "Shriram Krishnamurthi",
   "Kathryn S. McKinley",
   "Louis-Noël Pouchet",
   "Michael Pradel",
   "Shaz Qadeer",
   "Zhendong Su",
   "Saman P. Amarasinghe",
   "Ras Bodík",
   "Satish Chandra 0001",
   "Albert Cohen 0001",
   "Isil Dillig",
   "Ranjit Jhala",
   "James R. Larus",
   "Andrew C. Myers",
   "Erez Petrank",
   "Eran Yahav"};*/

  
    /*String[] authors_of_interest =
      {"Alan Jeffrey",
       "Andreas Podelski",
       "David A. Naumann",
       "Gavin M. Bierman",
       "P. Madhusudan",
       "Martin Erwig",
       "Matthew J. Parkinson",
       "Mayur Naik",
       "Nate Foster",
       "Nikhil Swamy",
       "Peter Selinger",
       "Scott Owens",
       "Suresh Jagannathan",
       "Swarat Chaudhuri",
       "Stephanie Weirich"};*/
  
    String[] authors_of_interest =
      {"Philip Wadler",
       "Steve Zdancewic",
       "Benjamin C. Pierce",
       "David Walker",
       "Zhendong Su",
       "Rastislav Bodík",
       "Matthias Felleisen",
       "Robert Harper 0001",
       "Thomas W. Reps",
       "Simon L. Peyton Jones",
       "Kathryn S. McKinley",
       "Shmuel Sagiv",
       "Martin Odersky",
       "Alexander Aiken",
       "Michael Hicks 0001",
       "Rupak Majumdar",
       "Jan Vitek",
       "Sumit Gulwani",
       "Martin C. Rinard",
       "Cormac Flanagan",
       "Peter Sewell"};

List<String> titles = findTitles(dblp, conf, authors_of_interest, 2015, 2020);

    //findShortestTitles(titles);
    //findLongestTitles(titles);

    //findAuthorLists(dblp, conf);

    //Map<Person, List<Integer>> years_of = findAuthorsByYear(dblp, authors_of_interest, conf, 2015, 2020);

    //printList(years_of);

    //findMostPapers(years_of, 8);

    //findMostConfs(years_of, 5);

    //findLongestVacation(years_of, 5);

    //findLongestCareer(years_of, 5);

    //findLongestStreak(years_of, 5, this_year);

    //findMostPapersPerConf(years_of, 2);
    
    System.out.println("Finished.");
  }
}
