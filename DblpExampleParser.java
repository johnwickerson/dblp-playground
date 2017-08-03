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

  static void findLongestName(DblpInterface dblp) {
    System.out.format("%s, finding longest name in dblp...\n",
		      getTimestamp());
    String longestName = null;
    int longestNameLength = 0;
    for (Person pers : dblp.getPersons()) {
      for (PersonName name : pers.getNames()) {
	if (name.getName().length() > longestNameLength) {
	  longestName = name.getName();
	  longestNameLength = longestName.length();
	}
      }
    }
    System.out.format("%s, %s (%d chars)\n\n",
		      getTimestamp(), longestName, longestNameLength);
  }

  static void findMostProlificAuthor(DblpInterface dblp) {
    System.out.format("%s, finding most prolific author...\n",
		      getTimestamp());
    String prolificAuthorName = null;
    int prolificAuthorCount = 0;
    for (Person pers : dblp.getPersons()) {
      int publsCount = pers.numberOfPublications();
      if (publsCount > prolificAuthorCount) {
	prolificAuthorCount = publsCount;
	prolificAuthorName = pers.getPrimaryName().getName();
      }
    }
    System.out.format("%s, %s, %d records\n\n",
		      getTimestamp(), prolificAuthorName,
		      prolificAuthorCount);
  }

  static void findMostCoauthors(DblpInterface dblp) {
    System.out.format("%s, finding author with most coauthors...\n",
		      getTimestamp());
    String connectedAuthorName = null;
    int connectedAuthorCount = 0;
    for (Person pers : dblp.getPersons()) {
      int coauthorCount = dblp.numberOfCoauthors(pers);
      if (coauthorCount > connectedAuthorCount) {
	connectedAuthorCount = coauthorCount;
	connectedAuthorName = pers.getPrimaryName().getName();
      }
    }
    System.out.format("%s, %s, %d coauthors\n\n",
		      getTimestamp(), connectedAuthorName,
		      connectedAuthorCount);
  }

  static void findJimGrayCoauthors(DblpInterface dblp) {
    System.out.format("%s, finding coauthors of Jim Gray...\n",
		      getTimestamp());
    Person don = dblp.getPersonByName("Jim Gray");
    for (int i = 0; i < dblp.numberOfCoauthorCommunities(don); i++) {
      Collection<Person> coauthors = dblp.getCoauthorCommunity(don, i);
      System.out.format("Group %d:\n", i);
      for (Person coauthor : coauthors) {
	System.out.format("  %s\n", coauthor.getPrimaryName().getName());
      }
    }
    System.out.format("%s, done\n\n", getTimestamp());
  }

  static Comparator<Person> person_cmp =
      (Person o1, Person o2) ->
      o1.getPrimaryName().getName().
      compareTo(o2.getPrimaryName().getName());
  
  static void findFOCS2010Authors(DblpInterface dblp) {
    System.out.format("%s, finding authors of FOCS 2010...\n",
		      getTimestamp());
    Map<Person, Integer> authors = new TreeMap(person_cmp);
    for (Publication publ : dblp.getPublications()) {
      FieldReader reader = publ.getFieldReader();
      if ("FOCS".equals(reader.valueOf("booktitle"))
	  && "2010".equals(reader.valueOf("year"))) {
	for (PersonName name : publ.getNames()) {
	  Person pers = name.getPerson();
	  if (authors.containsKey(pers))
	    authors.put(pers, authors.get(pers) + 1);
	  else
	    authors.put(pers, 1);
	}
      }
    }
    for (Person author : authors.keySet())
      System.out.format("  %dx %s\n", authors.get(author), author.getPrimaryName().getName());
    System.out.format("%s, done\n\n", getTimestamp());
  }
  
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
      System.out.format("%s:", p.getPrimaryName().getName());
      for (int y : years_of.get(p)) {
	System.out.format(" %d", y);
      }
      System.out.format("\n");
    }
    System.out.format("\n");
  }

  static void findMostPapers(Map<Person, List<Integer>> years_of) {
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
    for (Person p : ps) {
      int count = years_of.get(p).size();
      if (count > 1) {
	String name = p.getPrimaryName().getName();
	System.out.format("%d: %s\n", count, name);
      }
    }
    System.out.format("\n");
  }

  static void findLongestGap(Map<Person, List<Integer>> years_of) {
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
    for (Person p : ps) {
      int gap = gap_of.get(p);
      if (gap > 0) {
	String name = p.getPrimaryName().getName();
	System.out.format("%s (gap=%d)\n", name, gap);
      }
    }
    System.out.format("\n");
  }

  static void findLongestStreak(Map<Person, List<Integer>> years_of) {
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
    for (Person p : ps) {
      int streak = streak_of.get(p);
      if (streak > 1) {
	String name = p.getPrimaryName().getName();
	System.out.format("%s (streak=%d)\n", name, streak);
      }
    }
    System.out.format("\n");
  }

  static void findLongestRange(Map<Person, List<Integer>> years_of) {
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
    for (Person p : ps) {
      int range = range_of.get(p);
      if (range > 0) {
	String name = p.getPrimaryName().getName();
	System.out.format("%s (range=%d)\n", name, range);
      }
    }
    System.out.format("\n");
  }
  
  static void findMostYears(Map<Person, List<Integer>> years_of) {
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
    for (Person p : ps) {
      int count = new HashSet(years_of.get(p)).size();
      if (count >  1) {
	String name = p.getPrimaryName().getName();
	System.out.format("%d: %s\n", count, name);
      }
    }
    System.out.format("\n");
  }

  static void findMostPapersPerConf(Map<Person, List<Integer>> years_of) {
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
	if (prev_y == cur_y) {
	  cur_ppc = cur_ppc + 1;
	} else {
	  max_ppc = Math.max (max_ppc, cur_ppc);
	  cur_ppc = 1;
	}
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
    for (Person p : ps) {
      int ppc = ppc_of.get(p);
      if (ppc > 1) {
	String name = p.getPrimaryName().getName();
	System.out.format("%s (ppc=%d)\n", name, ppc);
      }
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

    /*
    findLongestName(dblp);

    findMostProlificAuthor(dblp);

    findMostCoauthors(dblp);

    findJimGrayCoauthors(dblp);

    findFOCS2010Authors(dblp);
    */

    Map<Person, List<Integer>> years_of = findAuthorsByYear(dblp, "FPGA");

    printList(years_of);

    findMostPapers(years_of);

    findMostYears(years_of);

    findLongestGap(years_of);

    findLongestRange(years_of);

    findLongestStreak(years_of);

    findMostPapersPerConf(years_of);
    
    System.out.println("Finished.");
  }
}
