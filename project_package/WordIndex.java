package project_package;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.io.IOException;


public class WordIndex{
	
	private RecordManager recman;
	private HTree hashtable_WordID;
	private HTree hashtable_Inverted;
	private HTree hashtable_Forward;
	private HTree hashtable_TitleInverted;

	public WordIndex(String recordmanager) throws IOException{
		recman = RecordManagerFactory.createRecordManager(recordmanager);
		long recid;
		
		recid = recman.getNamedObject("WordID");
		if (recid != 0)
			hashtable_WordID = HTree.load(recman, recid);
		else{
			hashtable_WordID = HTree.createInstance(recman);
			recman.setNamedObject("WordID", hashtable_WordID.getRecid() );
		}

		recid = recman.getNamedObject("Inverted");
		if (recid != 0)
			hashtable_Inverted = HTree.load(recman, recid);
		else{
			hashtable_Inverted = HTree.createInstance(recman);
			recman.setNamedObject("Inverted", hashtable_Inverted.getRecid() );
		}

		recid = recman.getNamedObject("Forward");
		if (recid != 0)
			hashtable_Forward = HTree.load(recman, recid);
		else{
			hashtable_Forward = HTree.createInstance(recman);
			recman.setNamedObject("Forward", hashtable_Forward.getRecid() );
		}

		recid = recman.getNamedObject("TitleInverted");
		if (recid != 0)
			hashtable_TitleInverted = HTree.load(recman, recid);
		else{
			hashtable_TitleInverted = HTree.createInstance(recman);
			recman.setNamedObject("TitleInverted", hashtable_TitleInverted.getRecid() );
		}
	}

	public void commit() throws IOException{
		recman.commit();
	}

	public void finalize() throws IOException{
		recman.commit();
		recman.close();				
	} 

	public void close() throws IOException{
		recman.close();
	}

	/**
	 * Function for hashtable_WordID.  
     * If a word have not been added to the index, add it and assign it a unique word ID.
     * @param word The word to be added.
     * @return The word ID of the word.
     */
    public UUID addWord(String word) throws IOException{
        java.lang.Object value = hashtable_WordID.get(word);
        if (value != null){
            return (UUID) value;
        }
		UUID uuid = UUID.nameUUIDFromBytes(word.getBytes());
		hashtable_WordID.put(word, uuid);
		return uuid;
    }

    /**
	 * Function for hashtable_WordID.  
     * Get the word ID of the word.
     * @param word The word want to get the word ID.
     * @return The word ID of the word.
     */
    public UUID getWordId(String word) throws IOException{
        return (UUID) hashtable_WordID.get(word);
    }

    /**
	 * Function for hashtable_WordID.  
     * Get the word of the word ID.
     * @param id The word ID want to get the word.
     * @return The word of the word ID.
     */
    public String getWord(UUID wordID) throws IOException{
        FastIterator iter = hashtable_WordID.keys();
        String key;
        while( (key = (String)iter.next())!=null){
            if (hashtable_WordID.get(key).equals(wordID)){
                return key;
            }
        }
        return null;
    }

	/**
	 * Function for hashtable_Forward.  
	 * add the wordID and positions of a pageID to the hashtable_Forward.
	 * @param PageID the pageID of the page that contains the word
	 * @param wordID the keyword id
	 * @param positions the vector of its postion in the page
	 */
	public void addForward(UUID PageID, UUID wordID, Vector<Integer> positions) throws IOException{
		java.lang.Object value = hashtable_Forward.get(PageID);
		Map<UUID, Vector<Integer>> forwardList;
		if (value == null){
			forwardList = new HashMap<UUID, Vector<Integer>>();
        }
        else{
			forwardList = (Map<UUID, Vector<Integer>>) value;
		}
		forwardList.put(wordID, positions);
		hashtable_Forward.put(PageID, forwardList);
	}

	/**
	 * Function for hashtable_Forward.  
	 * get the wordID and frequency of a pageID in the hashtable_Forward.  
	 * @param PageID the pageID of the page 
	 */
	public Map<UUID, Vector<Integer>> getForwardList(UUID PageID) throws IOException{
		return (Map<UUID, Vector<Integer>>)hashtable_Forward.get(PageID);
	}


	/**
	 * Function for hashtable_Inverted.  
	 * add the pageID and positions of the word to the hashtable_Inverted.
	 * @param wordID the keyword id
	 * @param PageID the pageID of the page that contains the word
	 * @param positions the vector of its postion in the page
	 */
	public void addInverted(UUID wordID, UUID PageID, Vector<Integer> positions) throws IOException{
		java.lang.Object value = hashtable_Inverted.get(wordID);
		Map<UUID, Vector<Integer>> postingList;
		if (value == null){
			postingList = new HashMap<UUID, Vector<Integer>>();
        }
        else{
			postingList = (Map<UUID, Vector<Integer>>) value;
		}

		postingList.put(PageID, positions);
		hashtable_Inverted.put(wordID, postingList);
	}

	/**
	 * Function for hashtable_Inverted.  
	 * get the posting list of a word in the hashtable_Inverted.
	 * @param wordID the wordID of the word 
	 */
	public Map<UUID, Vector<Integer>> getInvertedList(UUID wordID) throws IOException{
		return (Map<UUID, Vector<Integer>>)hashtable_Inverted.get(wordID);
	}

	/**
	 * Function for hashtable_TitleInverted.  
	 * add the pageID and positions of the word to the hashtable_Inverted.
	 * @param wordID the keyword id
	 * @param PageID the pageID of the page that contains the word
	 * @param positions the vector of its postion in the page
	 */
	public void addTitleInverted(UUID wordID, UUID PageID, Vector<Integer> positions) throws IOException{
		java.lang.Object value = hashtable_TitleInverted.get(wordID);
		Map<UUID, Vector<Integer>> postingList;
		if (value == null){
			postingList = new HashMap<UUID, Vector<Integer>>();
        }
        else{
			postingList = (Map<UUID, Vector<Integer>>) value;
		}

		postingList.put(PageID, positions);
		hashtable_TitleInverted.put(wordID, postingList);
	}

	/**
	 * Function for hashtable_TitleInverted.  
	 * get the posting list of a word in the hashtable_TitleInverted.
	 * @param wordID the wordID of the word 
	 */
	public Map<UUID, Vector<Integer>> getTitleInvertedList(UUID wordID) throws IOException{
		return (Map<UUID, Vector<Integer>>)hashtable_TitleInverted.get(wordID);
	}

	/**
	 * Function for hashtable_Forward.  
	 * Get the k highest frequency words in a page.
	 * @param PageID the pageID of the page 
	 * @param k the number of words want to get
	 * @return a map of wordID and frequency
	 */
	public Map<UUID, Integer> getHighestFrequencyWords(UUID pageID, int k) throws IOException{
		Map<UUID, Double> map1 = getHighestFrequencyWordsDouble(pageID, k);
		Map<UUID, Integer> map2 = convertDoubletoInt(map1);
		return map2;
	}	

	
	private <UUID, Integer extends Comparable<? super Integer>> Map<UUID, Double> getHighestFrequencyWordsDouble(UUID pageID, int k) throws IOException{
		Map<UUID, Vector<Integer>> vecMap = (Map<UUID, Vector<Integer>>) hashtable_Forward.get(pageID);

		Map<UUID, Double> map = new HashMap<UUID, Double>();
		for (Map.Entry<UUID, Vector<Integer>> entry : vecMap.entrySet()) {
			map.put(entry.getKey(), (double)entry.getValue().size());
		}

        Comparator<Map.Entry<UUID, Double>> valueComparator = new Comparator<Map.Entry<UUID, Double>>() {
            @Override
            public int compare(Map.Entry<UUID, Double> e1, Map.Entry<UUID, Double> e2) {
                return e2.getValue().compareTo(e1.getValue());
            }
        };

        PriorityQueue<Map.Entry<UUID, Double>> queue = new PriorityQueue<>(valueComparator);
        for (Map.Entry<UUID, Double> entry : map.entrySet()) {
            queue.offer(entry);
        }
        List<Map.Entry<UUID, Double>> result = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            Map.Entry<UUID, Double> entry = queue.poll();
            if (entry == null) {
                break;
            }
            result.add(entry);
        }
		Map<UUID, Double> highestFrequencyWords = result.stream()
    		.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return highestFrequencyWords;
    }

	private Map<UUID, Integer> convertDoubletoInt(Map<UUID, Double> highestFrequencyWords) throws IOException{
		Map<UUID, Integer> intMap = new HashMap<UUID, Integer>();
		for (Map.Entry<UUID, Double> entry : highestFrequencyWords.entrySet()) {
			intMap.put(entry.getKey(), entry.getValue().intValue());
		}
		return intMap;
	}

	/**
	 * Function for hashtable_Forward.  
     * Print all PageID and its word list.
     */
	public void printAllForward() throws IOException{
		FastIterator iter = hashtable_Forward.keys();
		UUID key;

		while( (key = (UUID)iter.next())!=null){
			Map<UUID, Vector<Integer>> dictionary = (Map<UUID, Vector<Integer>>) hashtable_Forward.get(key);
			System.out.println(key + " = "+ dictionary);
		}	
	}
	
	/**
	 * Function for hashtable_Inverted.  
     * Print all wordID and its posting.
     */
	public void printAllInverted() throws IOException{
		FastIterator iter = hashtable_Inverted.keys();
		UUID key;

		while( (key = (UUID)iter.next())!=null){
			Map<UUID, Vector<Integer>> dictionary = (Map<UUID, Vector<Integer>>) hashtable_Inverted.get(key);
			System.out.println(key + " = "+ dictionary);
		}	
	}

	/**
	 * Function for hashtable_TitleInverted.  
     * Print all wordID and its posting.
     */
	public void printAllTitleInverted() throws IOException{
		FastIterator iter = hashtable_TitleInverted.keys();
		UUID key;

		while( (key = (UUID)iter.next())!=null){
			Map<UUID, Vector<Integer>> dictionary = (Map<UUID, Vector<Integer>>) hashtable_TitleInverted.get(key);
			System.out.println(key + " = "+ dictionary);
		}	
	}

	/**
     * Function for hashtable_WordID.  
     * Print all word and its word ID.
     */
	public void printAllWordID() throws IOException{
		FastIterator iter = hashtable_WordID.keys();
		String key;
		while( (key = (String)iter.next())!=null){
			System.out.println(key + " = " + hashtable_WordID.get(key));
		}	
	}

	/**
	 * Function for hashtable_WordID and hashtable_Forward.  
	 * delete a page 
	 */
	public void delPage(UUID pageID) throws IOException{
		Map<UUID, Vector<Integer>> forwardList = getForwardList(pageID);
		if (forwardList != null){
			for (Map.Entry<UUID, Vector<Integer>> entry : forwardList.entrySet()) {
				Map<UUID, Vector<Integer>> invertedList = getInvertedList(entry.getKey());
				if (invertedList == null)
					continue;
				invertedList.remove(pageID);
				if (invertedList.isEmpty())
					hashtable_Inverted.remove(entry.getKey());
				else
					hashtable_Inverted.put(entry.getKey(), invertedList);
			}
		}

		hashtable_Forward.remove(pageID);
	}

}
