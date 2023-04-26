package project_package;

import IRUtilities.*;
import java.io.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Vector;

public class StopStem
{
	private Porter porter;
	private HashSet<String> stopWords;

	public StopStem(){
		super();
		porter = new Porter();
		stopWords = new HashSet<String>();

		// use bufferedReader to read line from stopwords.txt
		// add each line to HashSet<String> stopWords
		try(BufferedReader br = new BufferedReader(new FileReader("IRUtilities/stopwords.txt"))) {
		    for(String line; (line = br.readLine()) != null; ) {
		        stopWords.add(line);
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}

	public Vector<String> runStopStem(Vector<String> words){
		Vector<String> stemmedWords = new Vector<String>();
		for (String word : words){
			String stemmedWord = StopStemWord(word);
			if (stemmedWord != null){
				stemmedWords.add(stemmedWord);
			}
		}

		for (int i = 0; i < stemmedWords.size(); i++) {
            if (stemmedWords.get(i).isEmpty()) {
                stemmedWords.remove(i);
                i--;
            }
        }

		Vector<String> doubleCheck = new Vector<String>();
		for (String word : stemmedWords){
			String stemmedWord = StopStemWord(word);
			if (stemmedWord != null){
				doubleCheck.add(stemmedWord);
			}
		}

		return doubleCheck;
	}


	private String StopStemWord(String word){
		if (isStopWord(word)){
			return null;
		}
		else{
			return stem(word);
		}
	}
	

	private boolean isStopWord(String word){
		return stopWords.contains(word);	
	}


	private String stem(String word){
		return porter.stripAffixes(word);
	}
}
