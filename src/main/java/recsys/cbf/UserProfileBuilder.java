package recsys.cbf;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.history.UserHistory;

import javax.annotation.Nonnull;

/**
 * Builds a user profile from the user's ratings and the content model.  This is split
 * into a separate class so that we can have 2 different ones &mdash; weighted and
 * unweighted &mdash; and use the same code for the rest of the process.
 */
public interface UserProfileBuilder {
    /**
     * Create a user profile (weights over tags).
     *
     * @param history The user's history (their ratings).
     * @return A vector of tag weights describing the user's preferences.  The tag IDs are as per
     * the {@link recsys.cbf.TFIDFModel}.
     */
    Long2DoubleMap makeUserProfile(@Nonnull UserHistory<Rating> history);
}
