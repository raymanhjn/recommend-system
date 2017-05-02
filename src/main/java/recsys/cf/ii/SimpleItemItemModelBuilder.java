/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package recsys.cf.ii;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import org.lenskit.util.collections.LongUtils;
import org.lenskit.inject.Transient;
import org.lenskit.data.dao.ItemDAO;
import org.lenskit.data.dao.ItemEventDAO;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.ratings.Ratings;
import org.lenskit.data.history.ItemEventCollection;
import org.grouplens.lenskit.vectors.ImmutableSparseVector;
import org.lenskit.util.math.Vectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lenskit.util.io.ObjectStream;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class SimpleItemItemModelBuilder implements Provider<SimpleItemItemModel> {

    private static final Logger logger = LoggerFactory.getLogger(SimpleItemItemModelBuilder.class);
    private final ItemDAO itemDao;
    private final ItemEventDAO itemEventDAO;

    @Inject
    public SimpleItemItemModelBuilder(@Transient ItemDAO idao,
            @Transient ItemEventDAO iedao) {
        itemDao = idao;
        itemEventDAO = iedao;
    }

    @Override
    public SimpleItemItemModel get() {
        Map<Long, Long2DoubleMap> itemVectors = Maps.newHashMap();
        Long2DoubleMap itemMeans = new Long2DoubleOpenHashMap();
        ObjectStream<ItemEventCollection<Rating>> stream = itemEventDAO.streamEventsByItem(Rating.class);
        try {
            for (ItemEventCollection<Rating> item : stream) {
                Long2DoubleOpenHashMap ratings = new Long2DoubleOpenHashMap(Ratings.itemRatingVector(item));
                Long2DoubleOpenHashMap reviseRatings = new Long2DoubleOpenHashMap(Ratings.itemRatingVector(item));
                for(Map.Entry<Long,Double> entry:ratings.entrySet()){
                    if(entry.getValue()!=0&&entry.getValue()!=55){
                        reviseRatings.put(entry.getKey(), entry.getValue());
                    } 
                }
                // Store the item's mean rating in itemMeans
                itemMeans.put(item.getItemId(), Vectors.mean(reviseRatings));
                // Normalize the item vector before putting it in itemVectors
                ImmutableSparseVector isv =ImmutableSparseVector.create(reviseRatings);
                double n= isv.norm()==0?1:isv.norm();
                for (Map.Entry<Long, Double> entry : reviseRatings.entrySet()) {
                    entry.setValue(entry.getValue()/n);
                }
                itemVectors.put(item.getItemId(), LongUtils.frozenMap(reviseRatings));
            }
        } finally {
            stream.close();
        }

        LongSortedSet items = LongUtils.packedSet(itemVectors.keySet());

        // Create an object to store the similarity matrix
        Map<Long, Long2DoubleMap> itemSimilarities = Maps.newHashMap();
        for (long i : itemVectors.keySet()) {
            itemSimilarities.put(i, new Long2DoubleOpenHashMap());
        }

        // Compute similarities between each pair of items and
        for(long i:itemVectors.keySet()){
            Long2DoubleMap map=itemSimilarities.get(i);
            Long2DoubleMap targetVector=itemVectors.get(i);
            double targetEu= Vectors.euclideanNorm(targetVector);
            for(long j:itemVectors.keySet()){
                Long2DoubleMap itemVector=itemVectors.get(j);
                if(i!=j){
                    double dot=Vectors.dotProduct(targetVector, itemVector);
                    double itemEu= Vectors.euclideanNorm(itemVector);
                    System.out.println(targetEu);
                    double sim=dot/(targetEu*itemEu);
                    map.put(j, sim);
                }
            }
        }
        
        return new SimpleItemItemModel(LongUtils.frozenMap(itemMeans), itemSimilarities);
    }

    /**
     * Load the data into memory, indexed by item.
     *
     * @return A map from item IDs to item rating vectors. Each vector contains
     * users' ratings for the item, keyed by user ID.
     */
    public Map<Long, Long2DoubleMap> getItemVectors() {
        Map<Long, Long2DoubleMap> itemData = Maps.newHashMap();

        ObjectStream<ItemEventCollection<Rating>> stream = itemEventDAO.streamEventsByItem(Rating.class);
        try {
            for (ItemEventCollection<Rating> item : stream) {
                Long2DoubleMap vector = Ratings.itemRatingVector(item);

                // Compute and store the item's mean.
                double mean = Vectors.mean(vector);

                // Mean center the ratings.
                for (Map.Entry<Long, Double> entry : vector.entrySet()) {
                    entry.setValue(entry.getValue() - mean);
                }

                itemData.put(item.getItemId(), LongUtils.frozenMap(vector));
            }
        } finally {
            stream.close();
        }

        return itemData;
    }
}
