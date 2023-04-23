import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Vector;

import org.htmlparser.beans.StringBean;

public class Tester{

    public static void runCrawler() throws IOException{
        String url = "https://cse.hkust.edu.hk/";
        url = "https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm";
        int maxPage = 300;
        Crawler crawler = new Crawler();
        crawler.runCrawler(url, maxPage);
    }

    public static void setOutput(String filename) throws IOException{
        try {
            // Create a file to redirect output
            File file = new File(filename);
            
            // Create a print stream to write to the file
            PrintStream stream = new PrintStream(file);
            
            // Redirect the system output to the file
            System.setOut(stream);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void printURLPageID() throws IOException{
        System.out.println("\n------------------------------------- URL and its PageID -------------------------------------\n");
        URLIndex test = new URLIndex("URL");
        test.printAllPageID();
    }

    public static void printParentToChilden() throws IOException{
        System.out.println("\n------------------------------------- Parent and its Children -------------------------------------\n");
        URLIndex test = new URLIndex("URL");       
        test.printAllParentToChilden();
    }

    public static void printChildToParents() throws IOException{
        System.out.println("\n------------------------------------- Child and its Parents -------------------------------------\n");
        URLIndex test = new URLIndex("URL");
        test.printAllChildToParents();
    }

    public static void printPageTitle() throws IOException{
        System.out.println("\n------------------------------------- PageID and its PageTitle -------------------------------------\n");
        URLIndex test = new URLIndex("URL");
        test.printAllPageToTitle();
    }

    public static void printPageMeta() throws IOException{
        System.out.println("\n------------------------------------- PageID and its PageMeta -------------------------------------\n");
        URLIndex test = new URLIndex("URL");
        test.printAllPageMeta();
    }

    public static void printWordID() throws IOException{
        System.out.println("\n------------------------------------- Word and its ID -------------------------------------\n");
        WordIndex test = new WordIndex("WordDB");
        test.printAllWordID();
    }

    public static void printInverted() throws IOException{
        System.out.println("\n------------------------------------- Word and its PostingList -------------------------------------\n");
        WordIndex test = new WordIndex("WordDB");
        test.printAllInverted();
    }

    public static void printTitleInverted() throws IOException{
        System.out.println("\n------------------------------------- Word and its TitlePostingList -------------------------------------\n");
        WordIndex test = new WordIndex("WordDB");
        test.printAllTitleInverted();
    }

    public static void printForward() throws IOException{
        System.out.println("\n------------------------------------- Page and its forward word list -------------------------------------\n");
        WordIndex test = new WordIndex("WordDB");
        test.printAllForward();
    }

    public static void printSpiderResult() throws IOException{
        URLIndex urlIndex = new URLIndex("URL");
		WordIndex wordIndex = new WordIndex("WordDB");
        int displayNum = 10;

        Vector<UUID> proceedPage = urlIndex.getProceedPage();

        for (UUID pageID : proceedPage){
            System.out.println(urlIndex.getPageTitle(pageID));
            System.out.println(urlIndex.getPageURL(pageID));
            System.out.println(((PageMeta)urlIndex.getPageMeta(pageID)).getLastModified() + "; Size of page: " + ((PageMeta)urlIndex.getPageMeta(pageID)).getPageSize()+"; words after stop and stem: " + ((PageMeta)urlIndex.getPageMeta(pageID)).getPageSizeAfterStopStem()+"; Number of unique word: " + ((PageMeta)urlIndex.getPageMeta(pageID)).getPageSizeUnique());
            Map<UUID, Integer> wordList = wordIndex.getHighestFrequencyWords(pageID, displayNum);

            Map<String, Integer> wordList2 = new HashMap<String,Integer>();
            for (UUID wordID : wordList.keySet()){
                wordList2.put(wordIndex.getWord(wordID), wordList.get(wordID));
            }
            System.out.println("Top " + displayNum + " frequency words: " + wordList2);
            Vector<UUID>childenList = urlIndex.getParentToChilden(pageID);
            int i = 0;
            for (UUID child : childenList){
                if (i >= displayNum) break;
                System.out.println(urlIndex.getPageURL(child));
                i++;
            }

            System.out.println("---------------------------------------------------------------------------------------------------------------------------------");
        }
    }

    /**
     * Just for me to test new implementation.
     */
    public static void testing() throws Exception{
        StopStem stopStem = new StopStem();
        String url = "https://seng.hkust.edu.hk/";
        StringBean sb;
		Vector<String> v_word = new Vector<String>();

        sb = new StringBean ();
        sb.setLinks(false);
        sb.setURL (url);	
		StringTokenizer st = new StringTokenizer(sb.getStrings ());
		while(st.hasMoreTokens()) {
			v_word.addElement(st.nextToken());
		}
		System.out.println(v_word);
        System.out.println(stopStem.runStopStem(v_word));
    }

    /**
     * Mainline.
     * @param args The command line arguments.
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{

        if (args.length==0){
            System.out.println("Usage: check readme");
            System.exit(0);
        }

        switch (args[0]){
            case "-runCrawler":
                // setOutput("CrawlerResult.txt");
                runCrawler();
                break;
            case "-printAllURLdb":
                setOutput("AllURLdb.txt");
                printURLPageID();
                printParentToChilden();
                printChildToParents();
                printPageTitle();
                printPageMeta();
                break;
            case "-printPageTitle":
                setOutput("PageTitle.txt");
                printPageTitle();
                break;
            case "-printPageMeta":
                setOutput("PageMeta.txt");
                printPageMeta();
                break;
            case "-printURLPageID":
                setOutput("URLPageID.txt");
                printURLPageID();
                break;
            case "-printParentToChilden":
                setOutput("ParentToChilden.txt");
                printParentToChilden();
                break;
            case "-printChildToParents":
                setOutput("ChildToParents.txt");
                printChildToParents();
                break;
            case "-printWordID":
                setOutput("WordID.txt");
                printWordID();
                break;
            case "-printInverted":
                setOutput("Inverted.txt");
                printInverted();
                break;
            case "-printTitleInverted":
                setOutput("TitleInverted.txt");
                printTitleInverted();
                break;
            case "-printForward":
                setOutput("Forward.txt");
                printForward();
                break;
            case "-printAllWordDB":
                setOutput("AllWordDB.txt");
                printWordID();
                printInverted();
                printTitleInverted();
                printForward();
                break;
            case "-printSpiderResult":
                setOutput("spider_result.txt");
                printSpiderResult();
                break;
            case "-testing":
                testing();
                break;
            default:
                System.out.println("Usage: check readme.pdf");
                System.exit(0);
        }
    }
}