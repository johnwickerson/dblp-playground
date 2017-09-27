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
      if (confName.equals(reader.valueOf("booktitle"))) {
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
    int pos = 0;
    int prev_count = 0;
    for (Person p : ps) {
      int count = years_of.get(p).size();
      if (prev_count != count) pos++;
      prev_count = count;
      if (pos > max_pos && max_pos != 0) break;
      String name = p.getPrimaryName().getName();
      String url = p.getKey().replace("homepages", "http://dblp.org/pid");
      System.out.format("%d: %s [%s] (%d papers)\n",
			pos, name, url, count);
    }
    System.out.format("\n");
  }

  static void findLongestGap(Map<Person, List<Integer>> years_of,
			     int max_pos) {
    // Find authors with the longest gap
    System.out.format("%s, finding authors with the longest holiday...\n", getTimestamp());
    Map<Person, Integer> gap_of = new TreeMap(person_cmp);
    for (Person p : years_of.keySet()) {
      List<Integer> years = years_of.get(p);
      Collections.sort(years);
      int prev_year = years.get(0);
      int gap = 0;
      for (int cur_year : years) {
	gap = Math.max (gap, cur_year - prev_year);
	prev_year = cur_year;
      }
      gap_of.put(p,gap);
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
      if (prev_gap != gap) pos++;
      prev_gap = gap;
      if (pos > max_pos && max_pos != 0) break;
      String name = p.getPrimaryName().getName();
      String url = p.getKey().replace("homepages", "http://dblp.org/pid");
      System.out.format("%d: %s [%s] (%d year gap)\n  ",
			pos, name, url, gap);
      for (int y : years_of.get(p)) {
	System.out.format(" %d", y);
      }
      System.out.format("\n");
    }
    System.out.format("\n");
  }

  static void findLongestStreak(Map<Person, List<Integer>> years_of,
				int max_pos) {
    // Find authors with the longest streak
    System.out.format("%s, finding authors with the longest streak...\n", getTimestamp());
    Map<Person, Integer> streak_of = new TreeMap(person_cmp);
    for (Person p : years_of.keySet()) {
      List<Integer> years = years_of.get(p);
      Set<Integer> year_set = new HashSet(years);
      years = new ArrayList(year_set);
      Collections.sort(years);
      int longest_streak = 1;
      int current_streak = 1;
      for (int y : years) {
	if (years.contains(y + 1)) {
	  current_streak = current_streak + 1;
	} else {
	  longest_streak = Math.max (longest_streak, current_streak);
	  current_streak = 1;
	}
      }
      streak_of.put(p,longest_streak);
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
      if (prev_streak != streak) pos++;
      prev_streak = streak;
      if (pos > max_pos && max_pos != 0) break;
      String name = p.getPrimaryName().getName();
      String url = p.getKey().replace("homepages", "http://dblp.org/pid");
      System.out.format("%d: %s [%s] (streak of %d)\n  ",
			pos, name, url, streak);
      for (int y : years_of.get(p)) {
	System.out.format(" %d", y);
      }
      System.out.format("\n");
    }
    System.out.format("\n");
  }

  static void findLongestRange(Map<Person, List<Integer>> years_of,
			       int max_pos) {
    // Find authors with the longest range
    System.out.format("%s, finding authors with the longest range...\n", getTimestamp());
    Map<Person, Integer> range_of = new TreeMap(person_cmp);
    for (Person p : years_of.keySet()) {
      List<Integer> years = years_of.get(p);
      Collections.sort(years);
      int first_year = years.get(0);
      int last_year = years.get(years.size() - 1);
      range_of.put(p, last_year - first_year);
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
      if (prev_range != range) pos++;
      prev_range = range;
      if (pos > max_pos && max_pos != 0) break;
      String name = p.getPrimaryName().getName();
      String url = p.getKey().replace("homepages", "http://dblp.org/pid");
      System.out.format("%d: %s [%s] (range of %d years)\n  ",
			pos, name, url, range);
      for (int y : years_of.get(p)) {
	System.out.format(" %d", y);
      }
      System.out.format("\n");
    }
    System.out.format("\n");
  }
  
  static void findMostYears(Map<Person, List<Integer>> years_of,
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
    int pos = 0;
    int prev_count = 0;
    for (Person p : ps) {
      int count = new HashSet(years_of.get(p)).size();
      if (prev_count != count) pos++;
      prev_count = count;
      if (pos > max_pos && max_pos != 0) break;
      String name = p.getPrimaryName().getName();
      String url = p.getKey().replace("homepages", "http://dblp.org/pid");
      System.out.format("%d: %s [%s] (%d conferences)\n  ",
			pos, name, url, count);
      for (int y : years_of.get(p)) {
	System.out.format(" %d", y);
      }
      System.out.format("\n");
    }
    System.out.format("\n");
  }

  static void findMostPapersPerConf(Map<Person, List<Integer>> years_of,
				    int max_pos) {
    // Find authors with the most papers per conference
    System.out.format("%s, finding authors with the most papers per conference...\n", getTimestamp());
    Map<Person, Integer> ppc_of = new TreeMap(person_cmp);
    for (Person p : years_of.keySet()) {
      List<Integer> years = years_of.get(p);
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
	max_ppc = Math.max (max_ppc, cur_ppc);
	prev_y = cur_y;
      }
      ppc_of.put(p, max_ppc);
    }
    List<Person> ps = new ArrayList(years_of.keySet());
    Comparator<Person> compare_by_ppc =
      (Person p1, Person p2) -> {
      return Integer.compare(ppc_of.get(p2), ppc_of.get(p1));
    };
    ps.sort(compare_by_ppc);
    int pos = 0;
    int prev_ppc = 0;
    for (Person p : ps) {
      int ppc = ppc_of.get(p);
      if (prev_ppc != ppc) pos++;
      prev_ppc = ppc;
      if (pos > max_pos && max_pos != 0) break;
      String name = p.getPrimaryName().getName();
      String url = p.getKey().replace("homepages", "http://dblp.org/pid");
      System.out.format("%d: %s [%s] (%d papers in one conference)\n  ",
			pos, name, url, ppc);
      for (int y : years_of.get(p)) {
	System.out.format(" %d", y);
      }
      System.out.format("\n");
    }
    System.out.format("\n");
  }
  
  public static void main(String[] args) {
    
    // we need to raise entityExpansionLimit because the dblp.xml has millions of entities
    System.setProperty("entityExpansionLimit", "10000000");
    
    if (args.length != 1) {
      System.err.format("Usage: java %s <dblp-xml-file>\n", DblpExampleParser.class.getName());
      System.exit(0);
    }
    String dblpXmlFilename = args[0];
    
    DblpInterface dblp = mkInterface(dblpXmlFilename);

    Map<Person, List<Integer>> years_of = findAuthorsByYear(dblp, "PLDI");

    //printList(years_of);

    findMostPapers(years_of, 10);

    findMostYears(years_of, 10);

    findLongestGap(years_of, 5);

    findLongestRange(years_of, 5);

    findLongestStreak(years_of, 5);

    findMostPapersPerConf(years_of, 4);
    
    System.out.println("Finished.");
  }
}
