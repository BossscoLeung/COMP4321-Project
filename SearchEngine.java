import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.Vector;
import java.util.Map.Entry;

public class SearchEngine {
    String raw_query;
    Boolean containPhaseSearch;
    Vector<String> phaseSearch;
    Vector<String> queryWords;
    URLIndex urlIndex;
    WordIndex wordIndex;
    StopStem stopStem;
    final UUID ZERO_UUID = UUID.nameUUIDFromBytes(new byte[0]);

    public SearchEngine(String query) throws IOException {
        this.raw_query = query;
        this.containPhaseSearch = false;
        this.phaseSearch = null;
        this.queryWords = null;
        this.urlIndex = new URLIndex("URL");
		this.wordIndex = new WordIndex("WordDB");
        this.stopStem = new StopStem();
    }

    /*
     * return a map of page and the score of the page
     * return null if no page have the phase word
     */
    public Map<UUID, Double> search() throws IOException {
        parseQuery();
        Map<UUID, Double> unsortedMap = new HashMap<>();

        if (containPhaseSearch) {
            // search for phase search
            Map<UUID, Integer> pageContainPhase = getPageContainPhaseWords();
            Map<UUID, Integer> pageContainPhase_title = getPageContainPhaseWordsInTitle();
            // System.out.println("pageContainPhase: " + pageContainPhase + " " + pageContainPhase.size());
            // System.out.println("pageContainPhase_title: " + pageContainPhase_title + " " + pageContainPhase_title.size());
            if (pageContainPhase == null){
                return null;
            }
            Vector<UUID> allowedPage = new Vector<UUID>(pageContainPhase.keySet());

            Map<UUID, Double> queryTFIDF = calQueryTFIDFwithPhase(pageContainPhase);
            if (queryTFIDF == null) {
                return null;
            }
            // wordId = Map<pageID, TFIDF>
            Map<UUID, Map<UUID, Double>> pageTFIDF = new HashMap<>();

            for (UUID wordID : queryTFIDF.keySet()) {
                if (wordID != ZERO_UUID){
                    Map<UUID, Double> pageIDTFIDF = calTFIDFOfWordInPage(wordID);
                    Vector<UUID> removelist = new Vector<UUID>();
                    for (UUID pageID : pageIDTFIDF.keySet()) {
                        if (isTitleContainWord(pageID, wordID)) { // favor title contain word (1.5)
                            pageIDTFIDF.put(pageID, pageIDTFIDF.get(pageID)*1.5);
                        }
                        if(!allowedPage.contains(pageID)){
                            removelist.add(pageID);
                        }
                    }
                    for (UUID pageID : removelist) {
                        pageIDTFIDF.remove(pageID);
                    }
                    pageTFIDF.put(wordID, pageIDTFIDF);
                }
                else{
                    Map<UUID, Double> pageIDTFIDF = calTFIDFOfPhaseWordInPage(pageContainPhase);
                    for (UUID pageID : pageIDTFIDF.keySet()) {
                        if (pageContainPhase_title != null && pageContainPhase_title.get(pageID) != null) { // favor title contain phasesword (3*occurence)
                            pageIDTFIDF.put(pageID, pageIDTFIDF.get(pageID)*pageContainPhase_title.get(pageID)*3);
                        }
                    }
                    pageTFIDF.put(wordID, pageIDTFIDF);
                }
                
            }

            for (UUID wordID : queryTFIDF.keySet()) {
                Map<UUID, Double> pageIDTFIDF = pageTFIDF.get(wordID);
                for (UUID pageID : pageIDTFIDF.keySet()) {
                    if (unsortedMap.containsKey(pageID)) {
                        unsortedMap.put(pageID, unsortedMap.get(pageID) + pageIDTFIDF.get(pageID)*queryTFIDF.get(wordID));
                    } else {
                        unsortedMap.put(pageID, pageIDTFIDF.get(pageID)*queryTFIDF.get(wordID));
                    }
                }
            }

        } 
        else {
            // search for normal search
            Map<UUID, Double> queryTFIDF = calQueryTFIDF();
            if (queryTFIDF == null) return null;
            // wordId = Map<pageID, TFIDF>
            Map<UUID, Map<UUID, Double>> pageTFIDF = new HashMap<>();

            for (UUID wordID : queryTFIDF.keySet()) {
                Map<UUID, Double> pageIDTFIDF = calTFIDFOfWordInPage(wordID);
                // favor title contain word (1.5)
                for (UUID pageID : pageIDTFIDF.keySet()) {
                    if (isTitleContainWord(pageID, wordID)) {
                        pageIDTFIDF.put(pageID, pageIDTFIDF.get(pageID)*1.5);
                    }
                }
                pageTFIDF.put(wordID, pageIDTFIDF);
            }

            for (UUID wordID : queryTFIDF.keySet()) {
                Map<UUID, Double> pageIDTFIDF = pageTFIDF.get(wordID);
                for (UUID pageID : pageIDTFIDF.keySet()) {
                    if (unsortedMap.containsKey(pageID)) {
                        unsortedMap.put(pageID, unsortedMap.get(pageID) + pageIDTFIDF.get(pageID)*queryTFIDF.get(wordID));
                    } else {
                        unsortedMap.put(pageID, pageIDTFIDF.get(pageID)*queryTFIDF.get(wordID));
                    }
                }
            }

        }

        unsortedMap = log2Normaliztion(unsortedMap);
        Map<UUID, Double> sortedTreeMap = sortByValue(unsortedMap);
        return sortedTreeMap;
    }

    private void parseQuery() {
        if (raw_query.contains("\"")) {
            this.containPhaseSearch = true;
            this.phaseSearch = new Vector<String>(Arrays.asList(raw_query.split("\"")[1].split(" ")));
            this.phaseSearch = this.stopStem.runStopStem(this.phaseSearch);
            this.queryWords = new Vector<String>(Arrays.asList(raw_query.split("[^\\w]+")));
            if (this.queryWords.get(0).equals("")) {
                this.queryWords.remove(0);
            }
            this.queryWords = this.stopStem.runStopStem(this.queryWords);
            for (String word : this.phaseSearch) {
                this.queryWords.remove(word);
            }

        } else {
            this.queryWords = new Vector<String>(Arrays.asList(raw_query.split(" ")));
            this.queryWords = this.stopStem.runStopStem(this.queryWords);
        }
    }

    /*
     * return a map of page that contain phase word(in body and title) and the number of count
     */
    private Map<UUID, Integer> getPageContainPhaseWords() throws IOException{
        if (!this.containPhaseSearch) return null;
        Map<UUID, Integer> pageContainPhaseWordsWithcount = new HashMap<UUID, Integer>();
        Vector<UUID> pageContainPhaseWords = new Vector<UUID>();
        Vector<UUID> wordUuids = new Vector<UUID>();
        for(String word:this.phaseSearch){
            UUID wordID = this.wordIndex.getWordId(word);
            if (wordID == null) return null;
            wordUuids.add(wordID);
        }

        // get a vector of page that contain at least one of the word
        for(UUID wordID : wordUuids){
            Map<UUID, Vector<Integer>> value = this.wordIndex.getInvertedList(wordID);
            if (value == null) return null;
            Vector<UUID> pageContainWord = new Vector<>(value.keySet());
            pageContainPhaseWords.addAll(pageContainWord);
        }
        // convert Vector to a LinkedHashSet object.
        LinkedHashSet<UUID> tempLinkedHashSet = new LinkedHashSet<UUID>(pageContainPhaseWords);
        // clear the original Vector
        pageContainPhaseWords.clear();
        // add all elements of LinkedHashSet to the Vector
        pageContainPhaseWords.addAll(tempLinkedHashSet);
        System.out.println(pageContainPhaseWords.getClass());
        // check pageContainPhaseWords one by one
        for (UUID page : pageContainPhaseWords){
            // System.out.println("enter page body: " + page);
            Vector<Vector<Integer>> positions = new Vector<Vector<Integer>>();
            for(UUID wordID : wordUuids){
                Vector<Integer> value = this.wordIndex.getForwardList(page).get(wordID);
                if (value == null) {
                    // pageContainPhaseWords.remove(page);
                    break;
                }
                positions.add(value);
            }
            if (positions.size() != wordUuids.size()) {
                continue;
            }
            // System.out.println("positions: " + positions);
            int count = countContinuousSequence(positions);
            // System.out.println("body count: " + count);
            if (count > 0) {
                pageContainPhaseWordsWithcount.put(page, count);
            }
            // else{
            //     System.out.println("enter");
            //     pageContainPhaseWords.remove(page);
            //     System.out.println("exit");
            // }
        }

        return pageContainPhaseWordsWithcount.size()==0?null:pageContainPhaseWordsWithcount;
    }  

    /*
     * return a map of page that contain phase word(in title only) and the number of count
     */
    private Map<UUID, Integer> getPageContainPhaseWordsInTitle() throws IOException{
        if (!this.containPhaseSearch) return null;
        Map<UUID, Integer> pageContainPhaseWordsWithcount = new HashMap<UUID, Integer>();
        Vector<UUID> pageContainPhaseWords = new Vector<UUID>();
        Vector<UUID> wordUuids = new Vector<UUID>();
        for(String word:this.phaseSearch){
            UUID wordID = this.wordIndex.getWordId(word);
            if (wordID == null) return null;
            wordUuids.add(wordID);
        }

        // get a vector of page that contain at least one of the word
        for(UUID wordID : wordUuids){
            Map<UUID, Vector<Integer>> value = this.wordIndex.getTitleInvertedList(wordID);
            if (value == null) return null;
            Vector<UUID> pageContainWord = new Vector<>(value.keySet());
            pageContainPhaseWords.addAll(pageContainWord);
        }
        // convert Vector to a LinkedHashSet object.
        LinkedHashSet<UUID> tempLinkedHashSet = new LinkedHashSet<UUID>(pageContainPhaseWords);
        // clear the original Vector
        pageContainPhaseWords.clear();
        // add all elements of LinkedHashSet to the Vector
        pageContainPhaseWords.addAll(tempLinkedHashSet);

        // check pageContainPhaseWords one by one
        for (UUID page : pageContainPhaseWords){
            // System.out.println("enter page title: " + page);
            Vector<Vector<Integer>> positions = new Vector<Vector<Integer>>();
            for(UUID wordID : wordUuids){
                Vector<Integer> value = this.wordIndex.getTitleInvertedList(wordID).get(page);
                if (value == null) {
                    // pageContainPhaseWords.remove(page);
                    break;
                }
                positions.add(value);
            }
            if (positions.size() != wordUuids.size()) {
                continue;
            }
            int count = countContinuousSequence(positions);
            // System.out.println("title count: " + count);
            if (count > 0) {
                pageContainPhaseWordsWithcount.put(page, count);
            }
            // else{
            //     pageContainPhaseWords.remove(page);
            // }
        }

        return pageContainPhaseWordsWithcount.size()==0?null:pageContainPhaseWordsWithcount;
    }  

    /*
     * count the number of time a phase word appear in a page
     */
    private static int countContinuousSequence(Vector<Vector<Integer>> input) {
        if (input.size() == 1) {
            return input.get(0).size();
        }

        // Generate all combinations of integers
        Vector<Vector<Integer>> combinations = new Vector<>();
        generateCombinations(input, combinations, new Vector<>(), 0);
        
        // Check each combination for a continuous sequence
        int count = 0;
        for (Vector<Integer> combination : combinations) {
            if (isContinuousSequence(combination)) {
                count++;
            }
        }
        
        return count;
    }
    
    /*
     * helper function used to generate all combinations of phase word occurences
     */
    private static void generateCombinations(Vector<Vector<Integer>> input, Vector<Vector<Integer>> output, Vector<Integer> current, int index) {
        if (index == input.size()) {
            output.add(new Vector<>(current));
            return;
        }
        
        Vector<Integer> set = input.get(index);
        for (int i = 0; i < set.size(); i++) {
            current.add(set.get(i));
            generateCombinations(input, output, current, index + 1);
            current.remove(current.size() - 1);
        }
    }
    
    /*
     * check whether a vector of integers is a continuous sequence
     */
    private static boolean isContinuousSequence(Vector<Integer> input) {
        if (input.size() < 2) {
            return false;
        }
        
        int prev = input.get(0);
        for (int i = 1; i < input.size(); i++) {
            int current = input.get(i);
            if (current != prev + 1) {
                return false;
            }
            prev = current;
        }
        
        return true;
    }



    /*
     * use to sort sort the final score with descending order
     */
    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());
        Collections.reverse(list);

        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }


    /*
     * return a map of wordid in the query and its tfidf
     * min score is 0.5
     * max score is log2(N+1)
     */
    private Map<UUID, Double> calQueryTFIDFwithPhase(Map<UUID, Integer> phaseMap) throws IOException{
        Vector<UUID> wordUuids = new Vector<UUID>();
        Map<UUID, Integer> frequencyMap = new HashMap<>();

        if(this.queryWords != null){
            for(String word:this.queryWords){
                UUID wordID = this.wordIndex.getWordId(word);
                if (wordID == null) continue;
                wordUuids.add(wordID);
            }

            for (UUID s : wordUuids) {
                frequencyMap.put(s, frequencyMap.getOrDefault(s, 0) + 1);
            }
        }
        frequencyMap.put(ZERO_UUID, 1);
        
        int N = this.urlIndex.getNumOfIndexedPage();
        int max_tf = Collections.max(frequencyMap.values());

        Map<UUID, Double> queryTFIDF = new HashMap<UUID, Double>();
        for (UUID wordID : frequencyMap.keySet()){
            if(wordID != ZERO_UUID){
                int tf = frequencyMap.get(wordID);

                Map<UUID, Vector<Integer>> value = this.wordIndex.getInvertedList(wordID);
                if (value == null){ 
                    continue;
                }
                int df = value.size();
                double tfidf = (0.5 + 0.5*tf/max_tf)*log2(1+N/df);
                queryTFIDF.put(wordID, tfidf);
            }
            else{
                int tf = 1;
                int df = phaseMap.size();
                double tfidf = (0.5 + 0.5*tf/max_tf)*log2(1+N/df);
                queryTFIDF.put(wordID, tfidf);
            }

        }

        return queryTFIDF;
    }

    /*
     * return a map of wordid in the query and its tfidf
     * min score is 0.5
     * max score is log2(N+1)
     */
    private Map<UUID, Double> calQueryTFIDF() throws IOException{
        Vector<UUID> wordUuids = new Vector<UUID>();

        for(String word:this.queryWords){
            UUID wordID = this.wordIndex.getWordId(word);
            if (wordID == null) return null;
            wordUuids.add(wordID);
        }

        Map<UUID, Integer> frequencyMap = new HashMap<>();
        for (UUID s : wordUuids) {
            frequencyMap.put(s, frequencyMap.getOrDefault(s, 0) + 1);
        }
        
        int N = this.urlIndex.getNumOfIndexedPage();
        int max_tf = Collections.max(frequencyMap.values());

        Map<UUID, Double> queryTFIDF = new HashMap<UUID, Double>();
        for (UUID wordID : frequencyMap.keySet()){
            int tf = frequencyMap.get(wordID);

            Map<UUID, Vector<Integer>> value = this.wordIndex.getInvertedList(wordID);
            if (value == null){ 
                continue;
            }
            int df = value.size();
            double tfidf = (0.5 + 0.5*tf/max_tf)*log2(1+N/df);
            queryTFIDF.put(wordID, tfidf);
        }

        return queryTFIDF;
    }

    /*
     * return a map of pageID and its score of that word
     */
    private Map<UUID, Double> calTFIDFOfWordInPage(UUID wordID) throws IOException{
        Map<UUID, Double> returnMap = new HashMap<UUID, Double>();
        Map<UUID, Vector<Integer>> wordInvertedList = this.wordIndex.getInvertedList(wordID);
        int N = this.urlIndex.getNumOfIndexedPage();

        for (UUID pageID : wordInvertedList.keySet()){
            Map<UUID, Vector<Integer>> forwarsList = this.wordIndex.getForwardList(pageID);
            Map<UUID, Integer> wordfrequencyMap = new HashMap<>();
            for (Map.Entry<UUID, Vector<Integer>> entry : forwarsList.entrySet()) {
                UUID key = entry.getKey();
                Vector<Integer> vector = entry.getValue();
                int frequency = vector.size();
                wordfrequencyMap.put(key, frequency);
            }
            int max_tf = Collections.max(wordfrequencyMap.values());
            int tf = wordfrequencyMap.get(wordID);
            int df = wordInvertedList.size();
            double tfidf = (0.5 + 0.5*tf/max_tf)*log2(1+N/df);
            returnMap.put(pageID, tfidf);
        }

        return returnMap;
    }

    /*
     * return a map of pageID and its score of the phase word
     */
    private Map<UUID, Double> calTFIDFOfPhaseWordInPage(Map<UUID, Integer> pageContainPhase) throws IOException{
        Map<UUID, Double> returnMap = new HashMap<UUID, Double>();
        int N = this.urlIndex.getNumOfIndexedPage();

        for (UUID pageID : pageContainPhase.keySet()){
            Map<UUID, Vector<Integer>> forwarsList = this.wordIndex.getForwardList(pageID);
            Map<UUID, Integer> wordfrequencyMap = new HashMap<>();
            for (Map.Entry<UUID, Vector<Integer>> entry : forwarsList.entrySet()) {
                UUID key = entry.getKey();
                Vector<Integer> vector = entry.getValue();
                int frequency = vector.size();
                wordfrequencyMap.put(key, frequency);
            }
            int max_tf = Collections.max(wordfrequencyMap.values());
            int tf = pageContainPhase.get(pageID);
            int df = pageContainPhase.size();
            double tfidf = (0.5 + 0.5*tf/max_tf)*log2(1+N/df);
            returnMap.put(pageID, tfidf);
        }

        return returnMap;
    }

    private boolean isTitleContainWord(UUID pageID, UUID wordID) throws IOException{
        Map<UUID, Vector<Integer>> doclist = this.wordIndex.getTitleInvertedList(wordID);
        if (doclist == null) return false;
        if (doclist.containsKey(pageID)) return true;
        return false;
    }

    private Map<UUID, Double> normalizeScore(double maxScore, Map<UUID, Double> scoreMap){
        Map<UUID, Double> returnMap = new HashMap<UUID, Double>();
        for (UUID pageID : scoreMap.keySet()){
            double score = scoreMap.get(pageID);
            double normalizedScore = score/maxScore;
            returnMap.put(pageID, normalizedScore);
        }
        return returnMap;
    }

    private Map<UUID, Double> log2Normaliztion(Map<UUID, Double> scoreMap){
        Map<UUID, Double> returnMap = new HashMap<UUID, Double>();
        for (UUID pageID : scoreMap.keySet()){
            double score = scoreMap.get(pageID);
            double normalizedScore = log2(1+score);
            returnMap.put(pageID, normalizedScore);
        }
        return returnMap;
    }


    private static double log2(double n){
        return Math.log(n)/Math.log(2);
    }

    // testing main function
    public static void main(String[] args) throws IOException {
        // Vector<Vector<Integer>> test = new Vector<Vector<Integer>>();

        // Vector<Integer> vector = new Vector<Integer>(Arrays.asList(1,2,3,9,15));
        // test.add(vector);
        // vector = new Vector<Integer>(Arrays.asList(4));
        // test.add(vector);
        // vector = new Vector<Integer>(Arrays.asList(5,6,11));
        // test.add(vector);

        // Vector<Vector<Integer>> combinations = new Vector<>();
        // generateCombinations(test, combinations, new Vector<>(), 0);

        // System.out.println(combinations);
        // System.out.println("Sholde be true: "+countContinuousSequence(test));

        // test.clear();
        // vector = new Vector<Integer>(Arrays.asList(1,2,3,9,15));
        // test.add(vector);
        // vector = new Vector<Integer>(Arrays.asList(4,5,10));
        // test.add(vector);
        // vector = new Vector<Integer>(Arrays.asList(6,11));
        // test.add(vector);
        // combinations = new Vector<>();
        // generateCombinations(test, combinations, new Vector<>(), 0);

        // System.out.println(combinations);
        // System.out.println("Sholde be false: "+countContinuousSequence(test));

        // test.clear();
        // vector = new Vector<Integer>(Arrays.asList(1,9,15));
        // test.add(vector);
        // vector = new Vector<Integer>(Arrays.asList(2,16));
        // test.add(vector);
        // vector = new Vector<Integer>(Arrays.asList(3,17));
        // test.add(vector);
        // combinations = new Vector<>();
        // generateCombinations(test, combinations, new Vector<>(), 0);

        // System.out.println(combinations);
        // System.out.println("Sholde be false: "+countContinuousSequence(test));
        
        // input
        System.out.println("Enter your query:");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        scanner.close();

        SearchEngine se = new SearchEngine(input);
        // System.out.println(se.queryWords);
        Map<UUID, Double> result = se.search();
        System.out.println(result);
        Iterator<UUID> list = result.keySet().iterator();
        System.out.println(se.urlIndex.getPageTitle(list.next()));
        System.out.println(se.urlIndex.getPageTitle(list.next()));
        System.out.println(se.urlIndex.getPageTitle(list.next()));
        System.out.println(se.urlIndex.getPageTitle(list.next()));
    }

}
