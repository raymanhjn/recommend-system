package recsys.cbf;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.lenskit.data.dao.ItemDAO;
import org.lenskit.inject.Transient;
import recsys.dao.ItemTagDAO;
import org.lenskit.util.collections.LongUtils;
import org.lenskit.util.math.Vectors;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builder for computing {@linkplain TFIDFModel TF-IDF models} from item tag data.  This model stores
 * the items in our recommender in terms of the tag values (attributes) that describe them.  
 * Each item is represented by a normalized vector that encodes the item's description in 
 * tag space computed using TF-IDF, so tags applied more often to this item, or to fewer items
 * overall, have a higher score in the vector.  
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class TFIDFModelBuilder implements Provider<TFIDFModel> {
    private final ItemTagDAO itemTagDAO;
    private final ItemDAO itemDAO;

    /**
     * Construct a model builder.  The {@link Inject} annotation on this constructor tells LensKit
     * that it can be used to build the model builder.
     *
     * @param itdao The item-tag DAO.  This is where the builder will get access to item tags.
     * @param idao The item DAO. This is where the builder will get access to items.
     */
    @Inject
    public TFIDFModelBuilder(@Transient ItemTagDAO itdao,
                             @Transient ItemDAO idao) {
        this.itemTagDAO = itdao;
        this.itemDAO = idao;
    }

    /**
     * This method is where the model should actually be computed.
     * @return The TF-IDF model (a model that maps itemID to a TF-IDF tag vector).
     */
    @Override
    public TFIDFModel get() {
        // Build a map of tags to numeric IDs.  This lets you convert tags (which are strings)
        // into long IDs that you can use as keys in a tag vector.
        Map<String, Long> tagIds = buildTagIdMap();

        // Create a map to accumulate document frequencies for the IDF computation
        Long2DoubleMap docFreq = new Long2DoubleOpenHashMap();

        // We now proceed in 2 stages. First, we build a TF vector for each item.
        // While we do this, we also build the DF vector.
        // We will then apply the IDF to each TF vector and normalize it to a unit vector.

        // Create a map to store the item TF vectors.
        Map<Long,Long2DoubleMap> itemVectors = new HashMap<>();


        // Iterate over the items to compute each item's vector.
        LongSet items = itemDAO.getItemIds();
        for (long item: items) {
            // Create a work vector to accumulate this item's tag vector.
            Long2DoubleMap work = new Long2DoubleOpenHashMap();

            // TODO Populate the work vector with the number of times each tag is applied to this item.
            
            // TODO Increment the document frequency vector once for each unique tag on the item.

            // Save the vector in our map, we'll add IDF and normalize later.
            itemVectors.put(item, work);
        }

        // Now we've seen all the items, so we have each item's TF vector and a global vector
        // of document frequencies.
        // Invert and log the document frequency.  We can do this in-place.
        final double logN = Math.log(items.size());
        for (Map.Entry<Long,Double> e: docFreq.entrySet()) {
            docFreq.put((long)e.getKey(), logN - Math.log(e.getValue()));
        }

        // Now docFreq is a log-IDF vector.
        // So we can use it to apply IDF to each item vector to put it in the final model.
        // Create a map to store the final model data.
        Map<Long,Long2DoubleMap> modelData = new HashMap<>();
        for (Map.Entry<Long,Long2DoubleMap> entry: itemVectors.entrySet()) {
            Long2DoubleMap tv = new Long2DoubleOpenHashMap(entry.getValue());

            // TODO Convert this TF vector into a TF-IDF vector

            // TODO Normalize the TF-IDF vector to be a unit vector
            // HINT Take a look at the Vectors class in org.lenskit.util.math.
            
            // Store a frozen (immutable) version of the vector in the model data.
            modelData.put(entry.getKey(), LongUtils.frozenMap(tv));
        }

        return new TFIDFModel(tagIds, modelData);
    }

    /**
     * Build a mapping of tags to numeric IDs.
     *
     * @return A mapping from tags to IDs.
     */
    private Map<String,Long> buildTagIdMap() {
        // Get the universe of all tags
        Set<String> tags = itemTagDAO.getTagVocabulary();
        // Allocate our new tag map
        Map<String,Long> tagIds = Maps.newHashMap();

        for (String tag: tags) {
            // Map each tag to a new number.
            tagIds.put(tag, tagIds.size() + 1L);
        }
        return tagIds;
    }
}
