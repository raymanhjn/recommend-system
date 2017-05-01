package recsys.cbf;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.grouplens.grapht.annotation.DefaultProvider;
import org.lenskit.inject.Shareable;

import java.io.Serializable;
import java.util.Map;

/**
 * The model for a TF-IDF recommender.  The model just remembers the normalized tag vector for each
 * item.
 *
 * @see TFIDFModelBuilder
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
// LensKit models are annotated with @Shareable so they can be serialized and reused
@Shareable
// This model class will be built by the model builder
@DefaultProvider(TFIDFModelBuilder.class)
public class TFIDFModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Long> tagIds;
    private final Map<Long, Long2DoubleMap> itemVectors;

    /**
     * Constructor for the model.  This is package-private; the only way to build a model is with
     * the {@linkplain TFIDFModelBuilder model builder}.
     * <p>
     * In a LensKit model designed for a large data set, these would be optimized fastutil maps for
     * efficiency.
     *
     * @param tagIds A map of tags to their IDs.
     * @param itemVectors A map of item IDs to tag vectors.
     */
    TFIDFModel(Map<String,Long> tagIds, Map<Long,Long2DoubleMap> itemVectors) {
        this.tagIds = tagIds;
        this.itemVectors = itemVectors;
    }

    /**
     * Get a map of tags to their IDs.
     *
     * @return A map of tags to their IDs. This is the map computed by the model builder.
     */
    public Map<String,Long> getTagIds() {
        return tagIds;
    }

    /**
     * Get the tag vector for a particular item.
     *
     * @param item The item.
     * @return The item's tag vector.  If the item is not known to the model, then this vector is
     *         empty.
     */
    public Long2DoubleMap getItemVector(long item) {
        // Look up the item
        Long2DoubleMap vec = itemVectors.get(item);
        if (vec == null) {
            // We don't know the item! Return an empty vector
            return new Long2DoubleOpenHashMap();
        } else {
            return vec;
        }
    }
}
