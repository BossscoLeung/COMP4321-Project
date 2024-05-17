package project_package;

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
import java.util.regex.Pattern;

public class SearchEngine {
    String raw_query;
    Boolean containPhaseSearch;
    Vector<Vector<String>> phaseSearch;
    Vector<UUID> phaseIDs;
    Vector<String> queryWords;
    URLIndex urlIndex;
    WordIndex wordIndex;
    StopStem stopStem;

    public SearchEngine(String query) throws IOException {
        this.raw_query = query;
        this.containPhaseSearch = false;
        this.phaseSearch = null;
        this.queryWords = null;
        this.urlIndex = new URLIndex("URL");
		this.wordIndex = new WordIndex("WordDB");
        this.stopStem = new StopStem();
    }

    public String getQueryWords() {
        if(this.queryWords == null || this.queryWords.size() == 0) {
            return "";
        }
        String output = "";
        for (String word : this.queryWords) {
            output += word + ", ";
        }
        output = output.substring(0, output.length() - 2);
        return output;
    }

    public String getPhaseSearch() {
        if (!this.containPhaseSearch) {
            return "";
        }
        String output = "";
        for(Vector<String> phase : this.phaseSearch) {
            String inner = "";
            if (phase == null || phase.size() == 0) {
                continue;
            }
            for (String word : phase) {
                inner += word + " ";
            }
            inner = inner.substring(0, inner.length() - 1);
            output += inner + ", ";
        }
        output = output.substring(0, output.length() - 2);
        return output;
    }

    /*
     * return a map of page and the score of the page
     * return null if no page have the phase word
     */
    public Map<UUID, Double> search() throws IOException {
        Boolean successParse = parseQuery();
        if(!successParse) { // all query are stopped or any phase word are all stopped
            return null;
        }

        Map<UUID, Double> unsortedMap = new HashMap<>();
        Map<UUID, Double> queryTFIDF;
        if (containPhaseSearch) {
            // search for phase search

            Map<UUID, Vector<Integer>> pageContainPhase = getPageContainPhaseWords();
            Map<UUID, Vector<Integer>> pageContainPhase_title = getPageContainPhaseWordsInTitle();
            if (pageContainPhase == null){
                return null;
            }
            Vector<UUID> allowedPage = new Vector<UUID>();
            for (UUID pageID : pageContainPhase.keySet()) {
                if (!pageContainPhase.get(pageID).contains(0)) {
                    allowedPage.add(pageID);
                }
            }
            if (allowedPage.size() == 0) {
                return null;
            }

            queryTFIDF = calQueryTFIDFwithPhase(pageContainPhase);
            if (queryTFIDF == null) {
                return null;
            }
            // wordId = Map<pageID, TFIDF>
            Map<UUID, Map<UUID, Double>> pageTFIDF = new HashMap<>();

            for (UUID wordID : queryTFIDF.keySet()) {
                if (!this.phaseIDs.contains(wordID)){ // normal words
                    Map<UUID, Double> pageIDTFIDF = calTFIDFOfWordInPage(wordID);
                    Vector<UUID> removelist = new Vector<UUID>();
                    for (UUID pageID : pageIDTFIDF.keySet()) {
                        int occurence = getTitleContainWord(pageID, wordID);
                        if (occurence>0) { // favor title contain word (1.25*occurence)
                            pageIDTFIDF.put(pageID, pageIDTFIDF.get(pageID)*1.25*occurence);
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
                else{ // phase words
                    Map<UUID, Double> pageIDTFIDF = calTFIDFOfPhaseWordInPage(pageContainPhase, wordID);

                    // remove the mapping of pageID that not in allowedPage
                    Vector<UUID> removelist = new Vector<UUID>();
                    for (UUID pageID : pageIDTFIDF.keySet()) {
                        if(!allowedPage.contains(pageID)){
                            removelist.add(pageID);
                        }
                    }
                    for (UUID pageID : removelist) {
                        pageIDTFIDF.remove(pageID);
                    }

                    for (UUID pageID : pageIDTFIDF.keySet()) {
                        if (pageContainPhase_title != null){
                            Vector<Integer> value = pageContainPhase_title.get(pageID);
                            if (value != null) { // favor title contain phasesword (2*occurence)
                                int occurence  = value.get(this.phaseIDs.indexOf(wordID));
                                if (occurence>0) {
                                    pageIDTFIDF.put(pageID, pageIDTFIDF.get(pageID)*occurence*2);
                                }
                            }
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
                    } 
                    else {
                        unsortedMap.put(pageID, pageIDTFIDF.get(pageID)*queryTFIDF.get(wordID));
                    }
                }
            }

        } 
        else {
            // search for normal search
            queryTFIDF = calQueryTFIDF();
            if (queryTFIDF == null) return null;
            // wordId = Map<pageID, TFIDF>
            Map<UUID, Map<UUID, Double>> pageTFIDF = new HashMap<>();

            for (UUID wordID : queryTFIDF.keySet()) {
                Map<UUID, Double> pageIDTFIDF = calTFIDFOfWordInPage(wordID);
                for (UUID pageID : pageIDTFIDF.keySet()) {
                    int occurence = getTitleContainWord(pageID, wordID);
                    if (occurence>0) { // favor title contain word (1.25*occurence)
                        pageIDTFIDF.put(pageID, pageIDTFIDF.get(pageID)*1.25*occurence);
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

        unsortedMap = normaliztion(unsortedMap, queryTFIDF);
        Map<UUID, Double> sortedTreeMap = sortByValue(unsortedMap);

        // this.urlIndex.close();
        // this.wordIndex.close();
        return sortedTreeMap;
    }

    private Boolean parseQuery() {
        if (raw_query.contains("\"")) {
            this.containPhaseSearch = true;
            this.phaseIDs = new Vector<UUID>();
            phaseSearch = new Vector<Vector<String>>();

            Pattern pattern = Pattern.compile("\"([^\"]*)\"");
            java.util.regex.Matcher matcher = pattern.matcher(raw_query);
            while (matcher.find()) {
                Vector<String> temp = new Vector<String>(Arrays.asList(matcher.group(1).split(" ")));
                phaseSearch.add(temp);
            }

            String newString = raw_query.replace("\"", "");
            String[] arr = newString.split(" ");
            List<String> list = Arrays.asList(arr);
            this.queryWords = new Vector<>(list);
            this.queryWords = this.stopStem.runStopStem(this.queryWords);

            Boolean needexit = false;
            Vector<Vector<String>> temp = new Vector<Vector<String>>();
            for (Vector<String> phase : phaseSearch) {
                Vector<String> temp2 = this.stopStem.runStopStem(phase);
                if (temp2 == null || temp2.size() == 0) needexit = true;
                temp.add(temp2);
            }
            phaseSearch = temp;
            // remove phase from queryWords
            for (Vector<String> phase : phaseSearch) {
                for (String word : phase) {
                    this.queryWords.remove(word);
                }
            }
            if(needexit) return false;

            for (Vector<String> phase : this.phaseSearch) {
                String str = String.join(" ", phase);
                UUID uuid = UUID.nameUUIDFromBytes(str.getBytes());
                this.phaseIDs.add(uuid);
            }

        } else {
            this.queryWords = new Vector<String>(Arrays.asList(raw_query.split(" ")));
            this.queryWords = this.stopStem.runStopStem(this.queryWords);
            if(this.queryWords == null || this.queryWords.size() == 0) return false;
        }

        return true;
    }

    /*
     * return a map of page that contain phase word(in body and title) and the number of count of phase word in order
     * {pageID, [count of phase word in order]}
     */
    private Map<UUID, Vector<Integer>> getPageContainPhaseWords() throws IOException{
        if (!this.containPhaseSearch) return null;
        // {pageID, [count of phase word in order]}
        Map<UUID, Vector<Integer>> pageContainPhaseWordsWithcount = new HashMap<UUID, Vector<Integer>>();

        // phase[[],
        //       [],
        //       []]
        // outer is the no. of phase, inner is page that contain phase
        Vector<Vector<UUID>> pageContainPhaseWords = new Vector<Vector<UUID>>();

        // phase[[,],
        //       [,],
        //       [,,]]
        // outer is the no. of phase, inner is the no. of word in phase
        Vector<Vector<UUID>> wordUuids = new Vector<Vector<UUID>>();

        for (Vector<String> phase : this.phaseSearch) {
            Vector<UUID> wordUuidsInPhase = new Vector<UUID>();
            for (String word : phase) {
                UUID wordID = this.wordIndex.getWordId(word);
                if (wordID == null) {
                    return null; // word not found in the phase -> not match
                }
                wordUuidsInPhase.add(wordID);
            }
            wordUuids.add(wordUuidsInPhase);
        }

        // get a vector of page that contain at least one of the word
        for(Vector<UUID> wordUuidsInPhase : wordUuids){
            Vector<UUID> pageContainPhaseWord = new Vector<UUID>();
            for(UUID wordID : wordUuidsInPhase){
                Map<UUID, Vector<Integer>> value = this.wordIndex.getInvertedList(wordID);
                if (value == null) {
                    return null;
                }
                Vector<UUID> pageContainWord = new Vector<>(value.keySet());
                pageContainPhaseWord.addAll(pageContainWord);
            }
            pageContainPhaseWords.add(pageContainPhaseWord);
        }

        // remove duplicate same page
        Vector<Vector<UUID>> temp = new Vector<Vector<UUID>>();

        for(Vector<UUID> pageContainPhaseWord : pageContainPhaseWords){
            // convert Vector to a LinkedHashSet object.
            LinkedHashSet<UUID> tempLinkedHashSet = new LinkedHashSet<UUID>(pageContainPhaseWord);
            // add all elements of LinkedHashSet to the Vector
            Vector<UUID> temp2 = new Vector<UUID>(tempLinkedHashSet);
            temp.add(temp2);
        }
        pageContainPhaseWords = temp;

        for(Vector<UUID> pageContainPhaseWord : pageContainPhaseWords){
            for(UUID page : pageContainPhaseWord){
                Vector<Integer> empty = new Vector<Integer>();
                pageContainPhaseWordsWithcount.put(page, empty);
            }
        }

        // check pageContainPhaseWords one by one
        for (int i = 0; i< pageContainPhaseWords.size(); i++){ // num of phases
            // pageids that contain as least one of the word in the ith phase
            Vector<UUID> pageContainPhaseWord = pageContainPhaseWords.get(i);
            // phase wordids of the phase
            Vector<UUID> wordUuidsInPhase = wordUuids.get(i);

            for(UUID page : pageContainPhaseWord){
                Vector<Integer> countForThisPage = pageContainPhaseWordsWithcount.get(page);
                // if (countForThisPage == null) {
                //     countForThisPage = new Vector<Integer>();
                // }

                Vector<Vector<Integer>> positions = new Vector<Vector<Integer>>();
                for(UUID wordID : wordUuidsInPhase){
                    Vector<Integer> value = this.wordIndex.getForwardList(page).get(wordID);
                    if (value == null) {
                        break;
                    }
                    positions.add(value);
                }
                if (positions.size() != wordUuidsInPhase.size()) {
                    countForThisPage.add(0);
                    pageContainPhaseWordsWithcount.put(page, countForThisPage);
                    continue;
                }
                int countContinuousSequence = countContinuousSequence(positions);
                countForThisPage.add(countContinuousSequence);
                pageContainPhaseWordsWithcount.put(page, countForThisPage);
            }

            for (UUID pageID : pageContainPhaseWordsWithcount.keySet()){
                Vector<Integer> countForThisPage = pageContainPhaseWordsWithcount.get(pageID);
                // if (countForThisPage == null) {
                //     countForThisPage = new Vector<Integer>();
                // }
                if (countForThisPage.size() != i+1) {
                    countForThisPage.add(0);
                    pageContainPhaseWordsWithcount.put(pageID, countForThisPage);
                }
            }
        }
        return pageContainPhaseWordsWithcount.size() == 0 ? null:pageContainPhaseWordsWithcount;
    }  

    /*
     * return a map of page that contain phase word(in title only) and the number of count
     */
    private Map<UUID, Vector<Integer>> getPageContainPhaseWordsInTitle() throws IOException{
        if (!this.containPhaseSearch) return null;

        // {pageID, [count of phase word in order]}
        Map<UUID, Vector<Integer>> pageContainPhaseWordsWithcount = new HashMap<UUID, Vector<Integer>>();

        // phase[[],
        //       [],
        //       []]
        // outer is the no. of phase, inner is page that contain phase
        Vector<Vector<UUID>> pageContainPhaseWords = new Vector<Vector<UUID>>();

        // phase[[,],
        //       [,],
        //       [,,]]
        // outer is the no. of phase, inner is the no. of word in phase
        Vector<Vector<UUID>> wordUuids = new Vector<Vector<UUID>>();

        for (Vector<String> phase : this.phaseSearch) {
            Vector<UUID> wordUuidsInPhase = new Vector<UUID>();
            for (String word : phase) {
                UUID wordID = this.wordIndex.getWordId(word);
                if (wordID == null) return null; // word not found in the phase -> not match
                wordUuidsInPhase.add(wordID);
            }
            wordUuids.add(wordUuidsInPhase);
        }

        // get a vector of page that contain at least one of the word
        for(Vector<UUID> wordUuidsInPhase : wordUuids){
            Vector<UUID> pageContainPhaseWord = new Vector<UUID>();
            for(UUID wordID : wordUuidsInPhase){
                Map<UUID, Vector<Integer>> value = this.wordIndex.getTitleInvertedList(wordID);
                if (value == null) return null;
                Vector<UUID> pageContainWord = new Vector<>(value.keySet());
                pageContainPhaseWord.addAll(pageContainWord);
            }
            pageContainPhaseWords.add(pageContainPhaseWord);
        }

        // remove duplicate same page
        Vector<Vector<UUID>> temp = new Vector<Vector<UUID>>();

        for(Vector<UUID> pageContainPhaseWord : pageContainPhaseWords){
            // convert Vector to a LinkedHashSet object.
            LinkedHashSet<UUID> tempLinkedHashSet = new LinkedHashSet<UUID>(pageContainPhaseWord);
            // add all elements of LinkedHashSet to the Vector
            Vector<UUID> temp2 = new Vector<UUID>(tempLinkedHashSet);
            temp.add(temp2);
        }
        pageContainPhaseWords = temp;

        for(Vector<UUID> pageContainPhaseWord : pageContainPhaseWords){
            for(UUID page : pageContainPhaseWord){
                Vector<Integer> empty = new Vector<Integer>();
                pageContainPhaseWordsWithcount.put(page, empty);
            }
        }

        // check pageContainPhaseWords one by one
        for (int i = 0; i< pageContainPhaseWords.size(); i++){
            Vector<UUID> pageContainPhaseWord = pageContainPhaseWords.get(i);
            Vector<UUID> wordUuidsInPhase = wordUuids.get(i);
            for(UUID page : pageContainPhaseWord){
                Vector<Integer> countForThisPage = pageContainPhaseWordsWithcount.get(page);
                if (countForThisPage == null) {
                    countForThisPage = new Vector<Integer>();
                }

                Vector<Vector<Integer>> positions = new Vector<Vector<Integer>>();
                for(UUID wordID : wordUuidsInPhase){
                    Vector<Integer> value = this.wordIndex.getTitleInvertedList(wordID).get(page);
                    if (value == null) {
                        break;
                    }
                    positions.add(value);
                }
                if (positions.size() != wordUuidsInPhase.size()) {
                    countForThisPage.add(0);
                    pageContainPhaseWordsWithcount.put(page, countForThisPage);
                    continue;
                }
                int countContinuousSequence = countContinuousSequence(positions);
                countForThisPage.add(countContinuousSequence);
                pageContainPhaseWordsWithcount.put(page, countForThisPage);
            }

            for (UUID pageID : pageContainPhaseWordsWithcount.keySet()){
                Vector<Integer> countForThisPage = pageContainPhaseWordsWithcount.get(pageID);
                if (countForThisPage == null) {
                    countForThisPage = new Vector<Integer>();
                }
                if (countForThisPage.size() != i+1) {
                    countForThisPage.add(0);
                    pageContainPhaseWordsWithcount.put(pageID, countForThisPage);
                }
            }
        }
        return pageContainPhaseWordsWithcount.size() == 0 ? null:pageContainPhaseWordsWithcount;
    }  

    /*
     * count the number of time a phase word appear in a page
     */
    private static int countContinuousSequence(Vector<Vector<Integer>> input) {
        if(input == null || input.size() == 0) {
            return 0;
        }
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
     * 
     * Map<UUID, Integer>
     */
    private Map<UUID, Double> calQueryTFIDFwithPhase(Map<UUID, Vector<Integer>> phaseMap) throws IOException{
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

        for (UUID phaseID : this.phaseIDs) {
            wordUuids.add(phaseID);
            frequencyMap.put(phaseID, frequencyMap.getOrDefault(phaseID, 0) + 1);
        }
        
        int N = this.urlIndex.getNumOfIndexedPage();
        int max_tf = Collections.max(frequencyMap.values());

        Map<UUID, Double> queryTFIDF = new HashMap<UUID, Double>();
        for (UUID wordID : frequencyMap.keySet()){
            int tf = frequencyMap.get(wordID);

            if(!this.phaseIDs.contains(wordID)){
                Map<UUID, Vector<Integer>> value = this.wordIndex.getInvertedList(wordID);
                if (value == null){ 
                    continue;
                }
                int df = value.size();
                double tfidf = (0.5 + 0.5*tf/max_tf)*log2(1+N/df);
                queryTFIDF.put(wordID, tfidf);
            }
            else{
                int phaseNum = this.phaseIDs.indexOf(wordID);
                int df = 0;
                for (UUID page : phaseMap.keySet()) {
                    Vector<Integer> value = phaseMap.get(page);
                    if (value.get(phaseNum) != 0) {
                        df++;
                    }
                }
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
            if (wordID == null) continue;
            wordUuids.add(wordID);
        }

        if (wordUuids.size() == 0) {
            return null;
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
    private Map<UUID, Double> calTFIDFOfPhaseWordInPage(Map<UUID, Vector<Integer>> pageContainPhase, UUID wordID) throws IOException{
        Map<UUID, Double> returnMap = new HashMap<UUID, Double>();
        int N = this.urlIndex.getNumOfIndexedPage();

        int df = 0;
        for (UUID page : pageContainPhase.keySet()) {
            Vector<Integer> value = pageContainPhase.get(page);
            df+=value.get(this.phaseIDs.indexOf(wordID));
        }

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
            max_tf = Math.max(max_tf, Collections.max(pageContainPhase.get(pageID)) );

            int tf = pageContainPhase.get(pageID).get(this.phaseIDs.indexOf(wordID));
            double tfidf = (0.5 + 0.5*tf/max_tf)*log2(1+N/df) * tf * 1.15;
            returnMap.put(pageID, tfidf);
        }

        return returnMap;
    }

    private int getTitleContainWord(UUID pageID, UUID wordID) throws IOException{
        Map<UUID, Vector<Integer>> doclist = this.wordIndex.getTitleInvertedList(wordID);
        if (doclist == null) return 0;
        if (doclist.containsKey(pageID)){
            return doclist.get(pageID).size();
        }
        return 0;
    }

    private Map<UUID, Double> normaliztion(Map<UUID, Double> scoreMap, Map<UUID, Double> queryTFIDF) throws IOException{
        Map<UUID, Double> returnMap = new HashMap<UUID, Double>();
        // for (UUID pageID : scoreMap.keySet()){
        //     double score = scoreMap.get(pageID);
        //     double normalizedScore = log2(1+score);
        //     returnMap.put(pageID, normalizedScore);
        // }
        
        double queryLength = 0;
        for (UUID wordID : queryTFIDF.keySet()){
            queryLength += Math.pow(queryTFIDF.get(wordID), 2);
        }
        queryLength = Math.sqrt(queryLength);

        for (UUID pageID : scoreMap.keySet()){
            double score = scoreMap.get(pageID);
            double docLength = calDocLength(pageID);
            double normalizedScore = score / (queryLength*docLength);
            returnMap.put(pageID, normalizedScore);
        }
        
        return returnMap;
    }

    private double calDocLength(UUID pageID) throws IOException{
        Map<UUID, Vector<Integer>> forwardList = this.wordIndex.getForwardList(pageID);
        double docLength = 0;
        for (UUID wordID : forwardList.keySet()){
            int tf = forwardList.get(wordID).size();
            docLength += Math.pow(tf, 2);
        }
        docLength = Math.sqrt(docLength);
        return docLength;
    }


    private static double log2(double n){
        return Math.log(n)/Math.log(2);
    }

    // testing main function
    public static void main(String[] args) throws IOException {
        // input
        System.out.println("Enter your query:");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        scanner.close();

        SearchEngine se = new SearchEngine(input);   
        Map<UUID, Double> result = se.search();

        System.out.println(se.queryWords);
        System.out.println(se.phaseSearch);

        System.out.println(result);
        System.out.println(se.getQueryWords());
        System.out.println(se.getPhaseSearch());
        Iterator<UUID> list = result.keySet().iterator();
        System.out.println(se.urlIndex.getPageTitle(list.next()));
        System.out.println(se.urlIndex.getPageTitle(list.next()));
        System.out.println(se.urlIndex.getPageTitle(list.next()));
        System.out.println(se.urlIndex.getPageTitle(list.next()));
    }

}
