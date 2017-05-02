/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package recsys.dao;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import java.util.List;
import org.lenskit.data.dao.ItemDAO;
import org.lenskit.data.dao.UserDAO;

/**
 *
 * @author Jianing He
 */
public interface CusUserDAO extends UserDAO {
    
    
    Long2IntMap getUsersAge();
    
    int maxDifAge();
    
}
