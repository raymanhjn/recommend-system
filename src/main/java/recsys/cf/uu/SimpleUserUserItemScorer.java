package recsys.cf.uu;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import java.io.FileWriter;
import java.io.IOException;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.grouplens.lenskit.data.text.TextEventDAO;
import org.lenskit.data.dao.ItemNameDAO;
import org.lenskit.data.events.Event;
import static org.lenskit.util.math.Vectors.dotProduct;
import static org.lenskit.util.math.Vectors.euclideanNorm;
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
    private Long2IntMap usersAge;
    private int ageDif;
    private FileWriter fileWriter;
    
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
        String csvFile="data/output.csv";
        List<Rating> allRatings = getAllRatings();                     //all events includes train and test data
        Map<Long,List<Long>> predicts = getMarkedData(allRatings);     // user and books list that we need to predict
        usersAge = userInfoDao.getUsersAge();
        ageDif = userInfoDao.maxDifAge();
        List<Result> results = new ArrayList<>();
        Map<Long,Map<Long, Double>> printResult = new HashMap<>();
        for(Map.Entry<Long,List<Long>> entry : predicts.entrySet()){
            Long predictUser = entry.getKey();
            List<Long> predictItems=entry.getValue();
            Long2DoubleMap targetUserVector = getUserRatingVector(predictUser);   //already delete marked items
            Long2DoubleMap adjustedTargetUserVector = getAdjustedVector(targetUserVector);        //normalize target use vector;
            double targetUserMeanRating = getMean(targetUserVector);
            
            for(Long predictItem: predictItems){
                List<Rating> ratings = getTrainData(predictItem);             
                if(ratings != null){  // has other users rated this item
                    List<Long> neighbors = new ArrayList<>();
                    
                    for(int i = 0; i < ratings.size(); i++){
                        Long userId = ratings.get(i).getUserId();
                        if(userId != user){
                            neighbors.add(userId);
                        }             
                    }
                    
                    Long2DoubleMap userSimilarity = new Long2DoubleOpenHashMap();
                    for (Long nei : neighbors){
                        Long2DoubleMap neiVector = getUserRatingVector(nei);
                        Long2DoubleMap adjustedNeiVector = getAdjustedVector(neiVector);
                        double ageFactor = getAgeFactor(usersAge,ageDif,predictUser,nei);
                        Long2DoubleMap overlapVector = getOverlapRatingVector(adjustedTargetUserVector, adjustedNeiVector);
                        
                        if (overlapVector.keySet() != null && overlapVector.keySet().size() != 0){
                            Double similarity = getCosineSimilarity(adjustedTargetUserVector, overlapVector);
                            Double ageAdjustedSim=similarity*ageFactor;
                            if (!Double.isNaN(ageAdjustedSim)){
                                userSimilarity.put(nei, ageAdjustedSim);
                            }
                        } else {
                            Double similarity = getCosineSimilarity(adjustedTargetUserVector, adjustedNeiVector);
                            Double ageAdjustedSim=similarity*ageFactor;
                            if (!Double.isNaN(ageAdjustedSim)){
                                userSimilarity.put(nei, ageAdjustedSim);
                            }
                        }
                    }
                    
                    
                    // Sort the userSimilarity map in descending order
                    List<Map.Entry<Long,Double>> list=new ArrayList<>();  
                    list.addAll(userSimilarity.entrySet());  
                    SimpleUserUserItemScorer.ValueComparator vc=new ValueComparator();  
                    Collections.sort(list,vc);
                    
                    int count = 0;
                    double numerator = 0.00;
                    double denominator = 0.00;
                    double score = 0.00;
                    
                    // get the recommendation and prediction based on the top 30 most similar users' rating record
                    for (Map.Entry<Long, Double> l: list){
                        if (count >= 30){
                            break;
                        }
                        count++;
                        long userId = l.getKey();
                        double similarity = l.getValue();
                        
                        Long2DoubleMap userVector = getUserRatingVector(userId);
                        double meanRating = getMean(userVector);
                        double itemRating = userVector.get(predictItem);
                        
                        numerator += similarity * (itemRating - meanRating);
                        denominator += Math.abs(similarity);
                    }
                    
                    score = targetUserMeanRating + numerator / denominator;
                    if (Double.isNaN(score)){
                        if (!printResult.containsKey(predictUser)){
                            printResult.put(predictUser, new HashMap<Long, Double>());
                        }
                        printResult.get(predictUser).put(predictItem, targetUserMeanRating);
                    } else {
                        if (!printResult.containsKey(predictUser)){
                            printResult.put(predictUser, new HashMap<Long, Double>());
                        }
                        printResult.get(predictUser).put(predictItem, score);
                    }
                }
            }
        }
        
//        System.out.println(printResult.size());
//        int predictCount=0;
        try {
            fileWriter = new FileWriter(csvFile);
            for (Map.Entry<Long, Map<Long, Double>> pResult : printResult.entrySet()){
                Long predictUserId = pResult.getKey();
                Map<Long, Double> predictUserResult = pResult.getValue();
                for (Map.Entry<Long, Double> unitResult : predictUserResult.entrySet()){
                    Long predictItemId = unitResult.getKey();
                    Double predictItemRating = unitResult.getValue();
                    String line = predictUserId + "," + predictItemId + "," + predictItemRating+"\r\n";
                    fileWriter.write(line);
//                    predictCount++;
                }
            }
            fileWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(SimpleUserUserItemScorer.class.getName()).log(Level.SEVERE, null, ex);
        }

//        System.out.println("final result size " + predictCount); 
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
                if(rating.getValue() != 55){
                    results.add(rating);
                } 
            }
        }
        return results;
    }
    
    private Map<Long,List<Long>> getMarkedData(List<Rating> ratings){
        Map<Long,List<Long>> map=new HashMap<>();
        for(Rating r:ratings){
            if(r.hasValue() && r.getValue() == 55 ){
                Long user = r.getUserId();
                Long item = r.getItemId();
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
            if (r.hasValue() && r.getValue() != 55) {
               ratings.put(r.getItemId(), r.getValue());
             }
        }
        return ratings;
    }
    
    private Long2DoubleMap getAdjustedVector(Long2DoubleMap userVector){
            Long2DoubleMap adjustedVector = new Long2DoubleOpenHashMap();
            double mean = getMean(userVector);
            
            for (Map.Entry<Long, Double> entry: userVector.entrySet()){
                Long key = entry.getKey();
                Double value = entry.getValue() - mean;
                adjustedVector.put(key, value);
            }
            return adjustedVector;
        }
    
    private double getMean(Long2DoubleMap userVector){
            int count = 0;
            double sum = 0;
            
            for (Map.Entry<Long, Double> entry: userVector.entrySet()){
                sum += entry.getValue();
                count++;
            }
            return sum / count;
        }
    
    private Long2DoubleMap getOverlapRatingVector (Long2DoubleMap adjustedTargetUserVector, Long2DoubleMap adjustedNeiVector){
            Long2DoubleMap overlapVector = new Long2DoubleOpenHashMap();
            
            // find user and nei's common ratings on the same items
            for (Map.Entry<Long, Double> targetUserEntry: adjustedTargetUserVector.entrySet()){
                Long key = targetUserEntry.getKey();
                if (adjustedNeiVector.containsKey(key)){
                    overlapVector.put(key, adjustedNeiVector.get(key));
                }
            }
            return overlapVector;
    }
    
    private double getCosineSimilarity (Long2DoubleMap adjustedTargetUserVector, Long2DoubleMap overlapVector){
            //calculate dotProduct and Euclidean Norm to generate the final consine similarity
            double dotproduct = dotProduct(adjustedTargetUserVector, overlapVector);
            double euclidean = euclideanNorm(adjustedTargetUserVector) * euclideanNorm(overlapVector);
            double similarity = dotproduct / euclidean;
            
            return similarity;
    }
    
    private static class ValueComparator implements Comparator<Map.Entry<Long,Double>>  {  
            public int compare(Map.Entry<Long,Double> m, Map.Entry<Long,Double> n){  
                return  n.getValue().compareTo(m.getValue());  
            }  
    }
    
    private double getAgeFactor(Long2IntMap usersAge,int maxDif,long user1,long user2){
        int top = maxDif-Math.abs(usersAge.get(user1)-usersAge.get(user2));
        return top/maxDif;
    }
}