/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package recsys.dao;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.List;
import java.util.Map;
import org.lenskit.data.dao.ItemDAO;

/**
 *
 * @author Jianing He
 */
public interface CusBookDAO extends ItemDAO{
        Map<String,List<Long>> getBooks();
        
        Long2ObjectMap<String> getAuthor();

}
