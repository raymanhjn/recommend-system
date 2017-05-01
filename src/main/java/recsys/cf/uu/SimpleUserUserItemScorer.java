package recsys.cf.uu;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.ItemEventDAO;
import org.lenskit.data.dao.UserEventDAO;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.history.History;
import org.lenskit.data.history.UserHistory;
import org.lenskit.results.Results;
import org.lenskit.util.math.Vectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.lenskit.util.math.Vectors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.grouplens.lenskit.data.text.TextEventDAO;
import org.lenskit.data.dao.ItemNameDAO;
import org.lenskit.data.events.Event;
import recsys.dao.CSVBookDAO;
import recsys.dao.CSVUserDAO;

/**
* User-user item scorer.
* @author <a href="http://www.grouplens.org">GroupLens Research</a>
*/
public class SimpleUserUserItemScorer extends AbstractItemScorer {
    private final UserEventDAO userDao;
    private final ItemEventDAO itemDao;
    private final TextEventDAO eventDao;
    private final CSVUserDAO userInfoDao;
    private final CSVBookDAO bookInfoDao;
    
    @Inject
    public SimpleUserUserItemScorer(UserEventDAO udao, ItemEventDAO idao,TextEventDAO eDao,CSVUserDAO ufDao,CSVBookDAO bfDao) {
        userDao = udao;
        itemDao = idao;
        eventDao= eDao;
        userInfoDao = ufDao;
        bookInfoDao = bfDao;
    }
    
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        List<Rating> allRatings=getAllRatings();                   //all events includes train and test data
        Map<Long,List<Long>> predicts=getMarkedData(allRatings);     // user and books list that we need to predict
        
//        for(Map.Entry<Long,List<Long>> entry:predicts.entrySet()){
//            Long pUser=entry.getKey();
//            List<Long> pItems=entry.getValue();
//            Long2DoubleMap userVector = getUserRatingVector(pUser);   //already delete '55' marked items
//            Double pMean=Vectors.mean(userVector);
//            for(Long pItem:pItems){
//                List<Rating> ratings=getTrainData(pItem);
//                if(ratings!=null){  // has other users rated this item
//                    List<Long> users=new ArrayList<>();
//                    for(int i=0;i<ratings.size();i++){
//                        Long userId=ratings.get(i).getUserId();
//                        if(userId!=user) users.add(userId);            
//                    }
//                    Long2DoubleMap similarities=getSimilarity(pUser,users);
//                    Double top=0.00,bottom=0.00;
//                    
//                    for(Map.Entry<Long,Double> sEntry:similarities.entrySet()){
//                        double sim=sEntry.getValue();
//                        Long2DoubleMap uVector = getUserRatingVector(sEntry.getKey());
//                        Double mean=Vectors.mean(uVector);
//                        Double rat=uVector.get(pItem);
//                        top+=((rat-mean)*sEntry.getValue());
//                        bottom+=Math.abs(sEntry.getValue());
//                    }
//                    Double point=pMean+top/bottom; 
//                }
//            }
//        }
        
        String author =bookInfoDao.getAuthor( (long)78711040);
        System.out.println(author);
        List<Result> results = new ArrayList<>();  
        return Results.newResultMap(results);
    }
    
    private List<Rating> getAllRatings(){
        List<Rating> ratings=new ArrayList<>();
        Iterator<Rating> iterator=eventDao.streamEvents(Rating.class).iterator();
        while(iterator.hasNext()){
            Rating rating=iterator.next();
            ratings.add(rating);
        }
        return ratings;
    }
    
    private List<Rating> getTrainData(long item){
        List<Rating> ratings=itemDao.getEventsForItem(item, Rating.class);
        List<Rating> results=new ArrayList<>();
        if(ratings!=null){
            for(Rating rating:ratings){
                if(rating.getValue()!=55) results.add(rating);
            }
        }
        return results;
    }
    
    private Map<Long,List<Long>> getMarkedData(List<Rating> ratings){
        Map<Long,List<Long>> map=new HashMap<>();
        for(Rating r:ratings){
            if(r.hasValue() && r.getValue()==55 ){
                Long user=r.getUserId();
                Long item=r.getItemId();
                if(!map.containsKey(user)){
                    map.put(user, new ArrayList<Long>());
                }
                map.get(user).add(item);
            }
        }
        return map;
    }
    
    private Long2DoubleMap getUserRatingVector(long user) {
        UserHistory<Rating> history = userDao.getEventsForUser(user, Rating.class);
        if (history == null) {
            history = History.forUser(user);
        }
        Long2DoubleMap ratings = new Long2DoubleOpenHashMap();
        for (Rating r: history) {
            if (r.hasValue()&&r.getValue()!=55) {
               ratings.put(r.getItemId(), r.getValue());
             }
        }
        return ratings;
    }
    
    private Long2DoubleMap getSimilarity(long tUser, List<Long> users){
        Long2DoubleMap sim = new Long2DoubleOpenHashMap();
        Long2DoubleMap userRatings=getUserRatingVector(tUser);
        Double uMean=Vectors.mean(userRatings);
        for(Map.Entry<Long,Double> entry:userRatings.entrySet()){
            entry.setValue(entry.getValue()-uMean);
        }
        for(Long user:users) {
            Long2DoubleMap ratings=getUserRatingVector(user);
            Long2DoubleMap common=new Long2DoubleOpenHashMap();
            Long2DoubleMap rmm=new Long2DoubleOpenHashMap();
            Double rMean=Vectors.mean(ratings);
            //calculate euclidean of products in common for target user
            Double bottomLeft=0.00,bottomRight=0.00;  //two euclidean norms of (rating-average)
            for(long entry:ratings.keySet()){
                Double value=userRatings.get(entry);
                Double uValue=ratings.get(entry)-rMean;
                if(value!=null&&!value.equals(0.00)){
                    Double rValue=value-uMean;
                    common.put((Long)entry,rValue);
                    rmm.put((Long)entry,uValue);
                    bottomLeft+=(rValue*rValue);
                    bottomRight+=(uValue*uValue);
                }
            }
            Double dotpro=Vectors.dotProduct(common, rmm);
            Double similarity=dotpro/(Math.sqrt(bottomLeft)* Math.sqrt(bottomRight));                
            if(similarity>0) sim.put(user, similarity);
        }
        
            //sort the similarities and return top30 
            List<Map.Entry<Long,Double>> list = new ArrayList<>(sim.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<Long, Double>>(){
                @Override
                public int compare( Map.Entry<Long, Double> o1, Map.Entry<Long, Double> o2 )
                {
                    return ( o2.getValue() ).compareTo( o1.getValue() );
                }
            });
            Long2DoubleMap result=new Long2DoubleOpenHashMap();
            for(int i=0;i<list.size()&&i<30;i++){
                result.put(list.get(i).getKey(),list.get(i).getValue());
            }
            return result;
    }
    
}