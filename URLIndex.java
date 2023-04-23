import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;
import java.util.Vector;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

class PageMeta implements Serializable{
    private Date lastModified;
    private int pageSize;
    private int pageSizeAfterStopStem;
    private int pageSizeUnique;

    PageMeta(Date _lastModified, int _pageSize, int _pageSizeAfterStopStem, int _pageSizeUnique){
        lastModified = _lastModified;
        pageSize = _pageSize;
        pageSizeAfterStopStem = _pageSizeAfterStopStem;
        pageSizeUnique = _pageSizeUnique;
    }

    public Date getLastModified(){
        return lastModified;
    }

    public int getPageSize(){
        return pageSize;
    }

    public int getPageSizeAfterStopStem(){
        return pageSizeAfterStopStem;
    }

    public int getPageSizeUnique(){
        return pageSizeUnique;
    }

}

public class URLIndex
{
	private RecordManager recman;
	private HTree hashtable_PageID;
    private HTree hashtable_ParentToChilden;
    private HTree hashtable_ChildToParents;
    private HTree hashtable_PageToTitle;
    private HTree hashtable_PageMeta;

	URLIndex(String recordmanager) throws IOException
	{
		recman = RecordManagerFactory.createRecordManager(recordmanager);
		long recid;
		
        recid = recman.getNamedObject("PageID");
		if (recid != 0)
            hashtable_PageID = HTree.load(recman, recid);
		else{
			hashtable_PageID = HTree.createInstance(recman);
			recman.setNamedObject("PageID", hashtable_PageID.getRecid() );
		}

        recid = recman.getNamedObject("ParentToChilden");
		if (recid != 0)
            hashtable_ParentToChilden = HTree.load(recman, recid);
		else{
			hashtable_ParentToChilden = HTree.createInstance(recman);
			recman.setNamedObject("ParentToChilden", hashtable_ParentToChilden.getRecid() );
		}

        recid = recman.getNamedObject("ChildToParents");
		if (recid != 0)
            hashtable_ChildToParents = HTree.load(recman, recid);
		else{
			hashtable_ChildToParents = HTree.createInstance(recman);
			recman.setNamedObject("ChildToParents", hashtable_ChildToParents.getRecid() );
		}

        recid = recman.getNamedObject("PageToTitle");
		if (recid != 0)
            hashtable_PageToTitle = HTree.load(recman, recid);
		else{
			hashtable_PageToTitle = HTree.createInstance(recman);
			recman.setNamedObject("PageToTitle", hashtable_PageToTitle.getRecid() );
		}

        recid = recman.getNamedObject("PageMeta");
        if (recid != 0)
            hashtable_PageMeta = HTree.load(recman, recid);
        else{
            hashtable_PageMeta = HTree.createInstance(recman);
            recman.setNamedObject("PageMeta", hashtable_PageMeta.getRecid() );
        }
	}

	public void commit() throws IOException{
		recman.commit();
    }

	public void finalize() throws IOException{
		recman.commit();
		recman.close();				
	} 

    /**
     * Function for hashtable_PageID.  
     * If a URL have not been added to the index, add it and assign it a unique page ID.
     * @param URL The URL to be added.
     * @param lastModified The last modified date of the page. null if only adding the URL but not indexing the word.
     * @return  1: The URL is added. <br> 
     *          0: The URL is already in the index and no need update. <br>
     *          2: The URL is already in the index but need update.
     *          3: lastModified is null and the URL is already in the index.
     */
    public int addPage(String URL, Date lastModified) throws IOException{
        java.lang.Object value = hashtable_PageID.get(URL);
        if (value != null){
            if (lastModified == null){
                // lastModified is null and the URL is already in the index.
                return 3;
            }
            java.lang.Object meta = hashtable_PageMeta.get(value);
            if (meta == null){
                // The page is already in the index but not yet indexing the word.
                return 2;
            }
            if ( ((PageMeta)meta).getLastModified().compareTo(lastModified) < 0){
                // The page is already in the index but need update.
                return 2;
            }
            else{
                // The page is already in the index and no need update.
                return 0;
            }
        }
        else{
            UUID uuid = UUID.nameUUIDFromBytes(URL.getBytes());
            hashtable_PageID.put(URL, uuid);
            return 1;
        }
    }

    /**
     * Function for hashtable_PageToTitle.  
     * add the title of the page.
     * @param pageID The page ID.
     */
    public void addPageTitle(UUID pageID, String title) throws IOException{
        hashtable_PageToTitle.put(pageID, title);
    }

    /**
     * Function for hashtable_PageToTitle.  
     * Get the title of the page.
     * @param pageID The page ID.
     * @return The title of the page.
     */
    public String getPageTitle(UUID pageID) throws IOException{
        return (String) hashtable_PageToTitle.get(pageID);
    }

    /**
     * Function for hashtable_PageMeta.  
     * add the meta data of the page.
     * @param pageID The page ID.
     * @param lastModified The last modified date of the page.
     * @param pageSize The size of the page.
     * @param pageSizeAfterStopStem The size of the page after stop word and stemming.
     */
    public void addPageMeta(UUID pageID, Date lastModified, int pageSize, int pageSizeAfterStopStem, int pageSizeUnique) throws IOException{
        PageMeta pageMeta = new PageMeta(lastModified, pageSize, pageSizeAfterStopStem, pageSizeUnique);
        hashtable_PageMeta.put(pageID, pageMeta);
    }

    /**
     * Function for hashtable_PageMeta.  
     * Get the meta data of the page.
     * @param pageID The page ID.
     * @return The meta data of the page.
     */
    public PageMeta getPageMeta(UUID pageID) throws IOException{
        return (PageMeta) hashtable_PageMeta.get(pageID);
    }

    /**
     * Function for hashtable_PageID.  
     * Get the page ID of the URL.
     * @param URL The URL want to get the page ID.
     * @return The page ID of the URL.
     */
    public UUID getPageId(String URL) throws IOException{
        return (UUID) hashtable_PageID.get(URL);
    }

    public int getNumOfIndexedPage() throws IOException{
        return getProceedPage().size();
    }

    /**
     * Function for hashtable_PageID.  
     * Get the URL of the page ID.
     * @param pageID The page ID want to get the URL.
     * @return The URL of the page ID.
     */
    public String getPageURL(UUID pageID) throws IOException{
        FastIterator iter = hashtable_PageID.keys();
        String key;
        while( (key = (String)iter.next())!=null){
            if (hashtable_PageID.get(key).equals(pageID)){
                return key;
            }
        }
        return null;
    }

    /*
     * for knowing which page to print
     */
    public Vector<UUID> getProceedPage() throws IOException{
        Vector<UUID> proceedPage = new Vector<UUID>();
        FastIterator iter = hashtable_PageToTitle.keys();
        UUID key;
        while( (key = (UUID)iter.next())!=null){
            proceedPage.add(key);
        }
        return proceedPage;
    }

    /**
     * Function for hashtable_ParentToChilden.  
     * Add a child to the parent.
     * @param parent The parent page ID.
     * @param child The child page ID.
     */
    public void addParentToChilden(UUID parent, UUID child) throws IOException{
        java.lang.Object value = hashtable_ParentToChilden.get(parent);
        if (value == null){
            Vector<UUID> children = new Vector<UUID>();
            children.add(child);
            hashtable_ParentToChilden.put(parent, children);
        }
        else{
            if(!((Vector<UUID>)value).contains(child)){
                ((Vector<UUID>)value).add(child);
            }
            hashtable_ParentToChilden.put(parent, (Vector<UUID>)value);
        }
    }

    /**
     * Function for hashtable_ParentToChilden.  
     * Get the children of the parent.
     * @param parent The parent page ID.
     * @return The children'ID of the parent.
     */
    public Vector<UUID> getParentToChilden(UUID parent) throws IOException{
        if(hashtable_ParentToChilden.get(parent) == null)
            return null;
        else if(hashtable_ParentToChilden.get(parent) instanceof Vector)
            return (Vector<UUID>) hashtable_ParentToChilden.get(parent);
        return null;
    }

    /**
     * Function for hashtable_ChildToParents.  
     * Add a parent to a child.
     * @param parent The parent page ID.
     * @param child The child page ID.
     */
    public void addChildToParents(UUID child, UUID parent) throws IOException{
        java.lang.Object value = hashtable_ChildToParents.get(child);
        if (value == null){
            Vector<UUID> parents = new Vector<UUID>();
            parents.add(parent);
            hashtable_ChildToParents.put(child, parents);
        }
        else{
            if(!((Vector<UUID>)value).contains(parent)){
                ((Vector<UUID>)value).add(parent);
            }
            hashtable_ChildToParents.put(child, (Vector<UUID>)value);
        }
    }

    /**
     * Function for hashtable_ChildToParents.  
     * Get the parents of a child.
     * @param parent The child page ID.
     * @return The parents'ID of the child.
     */
    public Vector<UUID> getChildToParents(UUID child) throws IOException{
        if(hashtable_ChildToParents.get(child) == null)
            return null;
        else if(hashtable_ChildToParents.get(child) instanceof Vector)
            return (Vector<UUID>) hashtable_ChildToParents.get(child);
        return null;
    }

    /**
     * Function for hashtable_PageID.  
     * Print all the URL and page ID.
     */
	public void printAllPageID() throws IOException{
		FastIterator iter = hashtable_PageID.keys();
		String key;
		while( (key = (String)iter.next())!=null){
			System.out.println(key + " = " + hashtable_PageID.get(key));
		}	
	}	

    /**
     * Function for hashtable_ChildToParents.  
     * Print all Child and its parents.
     */
    public void printAllChildToParents() throws IOException{
		FastIterator iter = hashtable_ChildToParents.keys();
		UUID key;
		while( (key = (UUID)iter.next())!=null){
			System.out.println(key + " = " + (Vector<UUID>)hashtable_ChildToParents.get(key));
		}	
	}	

    /**
     * Function for hashtable_ParentToChilden.  
     * Print all parent and its childen.
     */
    public void printAllParentToChilden() throws IOException{
		FastIterator iter = hashtable_ParentToChilden.keys();
		UUID key;
		while( (key = (UUID)iter.next())!=null){
			System.out.println(key+ " = " + (Vector<UUID>)hashtable_ParentToChilden.get(key));
		}	
	}	

    /**
     * Function for hashtable_PageToTitle.  
     * Print all page and its title.
     */
    public void printAllPageToTitle() throws IOException{
        FastIterator iter = hashtable_PageToTitle.keys();
        UUID key;
        while( (key = (UUID)iter.next())!=null){
			System.out.println(key + " = " + (String)hashtable_PageToTitle.get(key));
		}	
    }

    /**
     * Function for hashtable_PageMeta.  
     * Print all page and its Metadata.
     */
    public void printAllPageMeta() throws IOException{
        FastIterator iter = hashtable_PageMeta.keys();
        UUID key;
        while( (key = (UUID)iter.next())!=null){
			System.out.println(key + " = " + ((PageMeta)hashtable_PageMeta.get(key)).getLastModified() + "; Size of page " + ((PageMeta)hashtable_PageMeta.get(key)).getPageSize()+"; Size of page after stop and stem " + ((PageMeta)hashtable_PageMeta.get(key)).getPageSizeAfterStopStem()+"; Size of unique word " + ((PageMeta)hashtable_PageMeta.get(key)).getPageSizeUnique());
		}	
    } 

    /**
     * delete a page
     */
    public void delPage(UUID pageID) throws IOException{
        Vector<UUID> parents = getChildToParents(pageID);
        Vector<UUID> children = getParentToChilden(pageID);
        
        // remove the pageId in hashtable_ParentToChilden for parent in parents
        if (parents != null){
            for(UUID parent: parents){
                Vector<UUID> parentChildren = getParentToChilden(parent);
                if (parentChildren == null)
                    continue;
                parentChildren.remove(pageID);
                if(parentChildren.size() == 0)
                    hashtable_ParentToChilden.remove(parent);
                else
                    hashtable_ParentToChilden.put(parent, parentChildren);
            }
        }
    
        // remove the pageId in hashtable_ChildToParents for child in children
        if (children != null){
            for(UUID child: children){
                Vector<UUID> childParents = getChildToParents(child);
                if (childParents == null)
                    continue;
                childParents.remove(pageID);
                if(childParents.size() == 0)
                    hashtable_ChildToParents.remove(child);
                else
                    hashtable_ChildToParents.put(child, childParents);
            }
        }
    
        hashtable_PageID.remove(getPageURL(pageID));
        hashtable_ChildToParents.remove(pageID);
        hashtable_ParentToChilden.remove(pageID);
        hashtable_PageToTitle.remove(pageID);
        hashtable_PageMeta.remove(pageID);
    }

    /**
     * delete a page but wont remove its parents link
     */
    public void updatePageCleaning(UUID pageID) throws IOException{
        Vector<UUID> children = getParentToChilden(pageID);
        
        // remove the pageId in hashtable_ChildToParents for child in children
        if (children != null){
            for(UUID child: children){
                Vector<UUID> childParents = getChildToParents(child);
                if (childParents == null)
                    continue;
                childParents.remove(pageID);
                if(childParents.size() == 0)
                    hashtable_ChildToParents.remove(child);
                else
                    hashtable_ChildToParents.put(child, childParents);
            }
        }
    
        hashtable_ParentToChilden.remove(pageID);
        hashtable_PageToTitle.remove(pageID);
        hashtable_PageMeta.remove(pageID);
    }
}
