package recsys.cbf;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import org.lenskit.api.Result;
import org.lenskit.api.ResultMap;
import org.lenskit.basic.AbstractItemScorer;
import org.lenskit.data.dao.UserEventDAO;
import org.lenskit.data.ratings.Rating;
import org.lenskit.data.history.UserHistory;
import org.lenskit.results.Results;
import org.lenskit.util.math.Vectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class TFIDFItemScorer extends AbstractItemScorer {
    private final UserEventDAO userEventDAO;
    private final TFIDFModel model;
    private final UserProfileBuilder profileBuilder;

    /**
     * Construct a new item scorer.  LensKit's dependency injector will call this constructor and
     * provide the appropriate parameters.
     *
     * @param uedao The user-event DAO, so we can fetch a user's ratings when scoring items for them.
     * @param m   The precomputed model containing the item tag vectors.
     * @param upb The user profile builder for building user tag profiles.
     */
    @Inject
    public TFIDFItemScorer(UserEventDAO uedao, TFIDFModel m, UserProfileBuilder upb) {
        this.userEventDAO = uedao;
        model = m;
        profileBuilder = upb;
    }

    /**
     * Generate item scores personalized for a particular user.  For the TFIDF scorer, this will
     * prepare a user profile and compare it to item tag vectors to produce the score.
     *
     * @param user   The user to score for.
     * @param items  A collection of item ids that should be scored.
     */
    @Override
    public ResultMap scoreWithDetails(long user, @Nonnull Collection<Long> items){
        // Get the user's ratings
        UserHistory<Rating> ratings = userEventDAO.getEventsForUser(user, Rating.class);

        if (ratings == null) {
            // the user doesn't exist, so return an empty ResultMap
            return Results.newResultMap();
        }

        // Create a place to store the results of our score computations
        List<Result> results = new ArrayList<>();

        // Get the user's profile, which is a vector with their preference score for each tag
        Long2DoubleMap userVector = profileBuilder.makeUserProfile(ratings);

        for (Long item: items) {
            double score = 0;
            Long2DoubleMap iv = model.getItemVector(item);

            // TODO Compute the cosine between this item's tag vector and the user's profile vector, and
            // TODO store it in the double variable score (which will be added to the result set).
            // HINT Take a look at the Vectors class in org.lenskit.util.math.
            
            // TODO And remove this exception to say you've implemented it
            //throw new UnsupportedOperationException("stub implementation");

            // Store the results of our score computation
            //results.add(Results.create(item, score));
        }

        return Results.newResultMap(results);
    }
}
































































