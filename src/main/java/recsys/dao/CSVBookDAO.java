/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package recsys.dao;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.apache.commons.lang3.text.StrTokenizer;
import org.lenskit.data.dao.DataAccessException;
import org.lenskit.util.io.LineStream;
import org.lenskit.util.io.ObjectStream;

/**
 *
 * @author Jianing He
 */
public class CSVBookDAO implements CusBookDAO{
    private final File bookFile;
    private transient volatile Long2ObjectMap<String> authors;
    private transient volatile Map<String,List<Long>> author2books;
    
    @Inject
    public CSVBookDAO(@BookFile File books){
        bookFile=books;
    }
    
    private void ensureUserCache(){
        if(bookFile !=null){
            synchronized (this) {
                if(bookFile !=null){
                    author2books = new HashMap<String,List<Long>>();
                    authors=new Long2ObjectOpenHashMap<String>();
                    ObjectStream<List<String>> lines = null;
                    try {
                        LineStream stream = LineStream.openFile(bookFile);
                        lines = stream.tokenize(StrTokenizer.getCSVInstance());
                    } catch (FileNotFoundException e) {
                        throw new DataAccessException("cannot open file", e);
                    }
                    try {
                        for(List<String> line:lines){
                            Long bookID=Long.parseLong(line.get(0));
                            String author=line.get(2);
                            authors.put(bookID, author);
                            if(!author2books.containsKey(author)){
                                author2books.put(author, new ArrayList<Long>());
                            }
                            author2books.get(author).add(bookID);
                        }
                    } finally {
                        lines.close();
                    }
                }
            }
        }
    }

    @Override
    public LongSet getItemIds() {
        ensureUserCache();
        return authors.keySet();
    }


    @Override
    public Map<String, List<Long>> getBooks() {
        ensureUserCache();
        return author2books;
    }

    @Override
    public Long2ObjectMap<String> getAuthor() {
        ensureUserCache();
        return authors;
    }
    
}
