package recsys.cbf;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.history.UserHistory;
import org.lenskit.util.collections.LongUtils;
import org.lenskit.util.math.Vectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Map;

/**
 * Build a user profile from all positive ratings.
 */
public class WeightedUserProfileBuilder implements UserProfileBuilder {
    /**
     * The tag model, to get item tag vectors.
     */
    private final TFIDFModel model;

    @Inject
    public WeightedUserProfileBuilder(TFIDFModel m) {
        model = m;
    }

    @Override
    public Long2DoubleMap makeUserProfile(@Nonnull UserHistory<Rating> history) {
        // Create a new vector over tags to accumulate the user profile
        Long2DoubleOpenHashMap profile = new Long2DoubleOpenHashMap();

        // TODO Normalize the user's ratings
        // TODO Build the user's weighted profile

        // The profile is accumulated, return it.
        // It is good practice to return a frozen vector.
        return LongUtils.frozenMap(profile);
    }
}
