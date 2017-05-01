import edu.umn.cs.recsys.cbf.TFIDFItemScorer

import edu.umn.cs.recsys.cbf.UserProfileBuilder

import edu.umn.cs.recsys.cbf.WeightedUserProfileBuilder

import edu.umn.cs.recsys.dao.CSVItemTagDAO

import edu.umn.cs.recsys.dao.ItemTagDAO

import edu.umn.cs.recsys.dao.TagFile

import org.lenskit.api.ItemScorer

import org.lenskit.data.dao.EventDAO

import org.lenskit.data.dao.ItemNameDAO

import org.grouplens.lenskit.data.text.ItemFile

import org.grouplens.lenskit.data.text.EventFile

import org.grouplens.lenskit.data.text.EventFormat

import org.grouplens.lenskit.data.text.Formats

import org.grouplens.lenskit.data.text.TextEventDAO

import org.grouplens.lenskit.data.text.CSVFileItemNameDAOProvider


// set up data access

bind ItemNameDAO toProvider CSVFileItemNameDAOProvider

set ItemFile to "data/movie-titles.csv"

bind EventDAO to TextEventDAO

set EventFile to "data/ratings.csv"

bind EventFormat to Formats.csvRatings()

bind ItemTagDAO to CSVItemTagDAO

set TagFile to "data/movie-tags.csv"


// finally, the core: use our item scorer

bind ItemScorer to TFIDFItemScorer

// with the basic profile builder

bind UserProfileBuilder to WeightedUserProfileBuilder
