package recsys.cf.ii;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.grouplens.lenskit.util.ScoredItemAccumulator;
import org.grouplens.lenskit.util.TopNScoredItemAccumulator;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.UserEventDAO;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.history.History;
import org.lenskit.data.history.UserHistory;
import org.lenskit.knn.NeighborhoodSize;
import org.lenskit.results.Results;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.lenskit.util.math.Vectors;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemScorer extends AbstractItemScorer {
    private final SimpleItemItemModel model;
    private final UserEventDAO userEvents;
    private final int neighborhoodSize;

    @Inject
    public SimpleItemItemScorer(SimpleItemItemModel m, UserEventDAO dao,
                                @NeighborhoodSize int nnbrs) {
        model = m;
        userEvents = dao;
        neighborhoodSize = nnbrs;
    }

    /**
     * Score items for a user.
     * @param user The user ID.
     * @param items The score vector.  Its key domain is the items to score, and the scores
     *               (rating predictions) should be written back to this vector.
     */
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items) {
        Long2DoubleMap itemMeans = model.getItemMeans();
        Long2DoubleMap ratings = getUserRatingVector(user);
        // TODO: Normalize the user's ratings by subtracting the item mean.
        for(Map.Entry<Long,Double> entry:ratings.entrySet()){
            long itemID=entry.getKey();
            double mean=itemMeans.get(itemID);
            entry.setValue(entry.getValue()-mean);
        }
        
        Long2DoubleMap scores = new Long2DoubleOpenHashMap();
        
        for (Long item: items ) {
            Long2DoubleMap allNeighbors = model.getNeighbors(item);
            List<Map.Entry<Long,Double>> list = new ArrayList<>(allNeighbors.entrySet());
            TopNScoredItemAccumulator accumulator=new TopNScoredItemAccumulator(10);
            //find top 30 neighbor items that user have rated
            Collections.sort(list, new Comparator<Map.Entry<Long, Double>>(){
                    @Override
                    public int compare( Map.Entry<Long, Double> o1, Map.Entry<Long, Double> o2 )
                    {
                        return ( o2.getValue() ).compareTo( o1.getValue() );
                    }
            });
            Long2DoubleMap topNeighbors=new Long2DoubleOpenHashMap();
            int i=0,count=0;
            while(count<30&&i<list.size()){
                long itemID= list.get(i).getKey();
                if(ratings.containsKey(itemID)){
                    topNeighbors.put(list.get(i).getKey(), list.get(i).getValue());
                    count++;
                }
                i++;
            }
            
            double top=0.00,bottom=0.00;
            for(Map.Entry<Long,Double> entry:topNeighbors.entrySet()){
                //entry's key is the itemID,value is similarity
                double rating=entry.getValue();
                if(ratings.get(entry.getKey())!=null){
                    top+= ratings.get(entry.getKey())*rating;
                    bottom+=Math.abs(rating);
                }
            }
            double score=itemMeans.get(item)+top/bottom;
            scores.put(item.longValue(), score);
            accumulator.put(item.longValue(), score);
        }
        List<Result> results = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : scores.entrySet()) {
            results.add(Results.create(entry.getKey(), entry.getValue()));
        }
        return Results.newResultMap(results);

    }

    /**
     * Get a user's ratings.
     * @param user The user ID.
     * @return The ratings to retrieve.
     */
    private Long2DoubleOpenHashMap getUserRatingVector(long user) {
        UserHistory<Rating> history = userEvents.getEventsForUser(user, Rating.class);
        if (history == null) {
            history = History.forUser(user);
        }

        Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap();
        for (Rating r: history) {
            if (r.hasValue()) {
                ratings.put(r.getItemId(), r.getValue());
            }
        }

        return ratings;
    }


}
