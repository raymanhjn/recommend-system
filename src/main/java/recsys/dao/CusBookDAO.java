/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package recsys.dao;

import java.util.List;
import org.lenskit.data.dao.ItemDAO;

/**
 *
 * @author Jianing He
 */
public interface CusBookDAO extends ItemDAO{
    
        List<Long> getBooksByAuthor(String author);
        
        String getAuthor(Long bookID);
}
