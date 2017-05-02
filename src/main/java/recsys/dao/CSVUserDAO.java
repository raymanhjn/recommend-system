/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package recsys.dao;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.apache.commons.lang3.text.StrTokenizer;
import org.lenskit.data.dao.DataAccessException;
import org.lenskit.data.dao.ItemDAO;
import org.lenskit.util.io.LineStream;
import org.lenskit.util.io.ObjectStream;
import org.lenskit.util.math.Vectors;

/**
 *
 * @author Jianing He
 */
public class CSVUserDAO implements CusUserDAO{
    private final File userFile;
    private transient volatile Map<String,List<Long>> authorCache;
    private transient volatile Long2IntMap userAge;
    private transient volatile int maxAge;
    private transient volatile int minAge;
    
    @Inject
    public CSVUserDAO(@UserFile File users){
        userFile=users;
    }
    
    private void ensureUserCache() {
        if(userFile !=null){
            synchronized (this) {
                if(userFile !=null){
                    authorCache = new HashMap<String,List<Long>>();
                    userAge=new Long2IntOpenHashMap();
                    ObjectStream<List<String>> lines = null;
                    try {
                        LineStream stream = LineStream.openFile(userFile);
                        lines = stream.tokenize(StrTokenizer.getCSVInstance());
                    } catch (FileNotFoundException e) {
                        throw new DataAccessException("cannot open file", e);
                    }
                    try {
                        for(List<String> line:lines){
                            int age;
                            long userID=Long.parseLong(line.get(0));
                            try{
                                age =Integer.parseInt(line.get(2));
                                maxAge=Math.max(maxAge,age);
                                minAge=Math.min(minAge,age);
                            }catch (NumberFormatException e) {
                                age=34;
                            }
                            userAge.put(userID, age);
                        }
                    } finally {
                        lines.close();
                    }
                }
            }
        }
    }
    

    @Override
    public LongSet getUserIds() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Long2IntMap getUsersAge() {
        ensureUserCache();
        return userAge;
    }

    @Override
    public int maxDifAge() {
        return maxAge-minAge;
    }


    
}
