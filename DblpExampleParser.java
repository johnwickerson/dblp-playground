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
  
  static Map<Person, List<Integer>>
    findAuthorsByYear(DblpInterface dblp, String confName) {
    Map<Person, List<Integer>> years_of = new TreeMap(person_cmp);
    
    for (Publication publ : dblp.getPublications()) {
      FieldReader reader = publ.getFieldReader();
      if (confName.equals(reader.valueOf("booktitle")) ||
	  confName.equals(reader.valueOf("number"))) {
	int year = Integer.parseInt(reader.valueOf("year"));
	for (PersonName name : publ.getNames()) {
	  Person pers = name.getPerson();
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

    String conf = "FPGA";
    int this_year = 2019;
    
    // we need to raise entityExpansionLimit because the dblp.xml has millions of entities
    System.setProperty("entityExpansionLimit", "10000000");
    
    if (args.length != 1) {
      System.err.format("Usage: java %s <dblp-xml-file>\n", DblpExampleParser.class.getName());
      System.exit(0);
    }
    String dblpXmlFilename = args[0];
    
    DblpInterface dblp = mkInterface(dblpXmlFilename);

    Map<Person, List<Integer>> years_of = findAuthorsByYear(dblp, conf);

    //printList(years_of);

    findMostPapers(years_of, 10);

    findMostConfs(years_of, 10);

    findLongestVacation(years_of, 5);

    findLongestCareer(years_of, 5);

    findLongestStreak(years_of, 5, this_year);

    findMostPapersPerConf(years_of, 2);
    
    System.out.println("Finished.");
  }
}
