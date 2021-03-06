import recsys.cf.uu.SimpleUserUserItemScorer
import org.lenskit.api.ItemScorer
import org.lenskit.data.dao.ItemDAO
import org.lenskit.data.dao.UserDAO
import org.lenskit.data.dao.EventDAO
import org.lenskit.data.dao.ItemNameDAO
import org.grouplens.lenskit.data.text.ItemFile
import org.grouplens.lenskit.data.text.EventFile
import org.grouplens.lenskit.data.text.EventFormat
import org.grouplens.lenskit.data.text.Formats
import org.grouplens.lenskit.data.text.TextEventDAO
import org.grouplens.lenskit.data.text.CSVFileItemNameDAOProvider
import recsys.dao.CSVUserDAO;
import recsys.dao.UserFile;
import recsys.dao.CSVBookDAO;
import recsys.dao.BookFile;

// use our item scorer
bind ItemScorer to SimpleUserUserItemScorer
// set up data access
bind UserDAO to CSVUserDAO
set UserFile to "data/BX-Users.csv"
bind ItemDAO to CSVBookDAO
set BookFile to "data/BX-Books.csv"
bind EventDAO to TextEventDAO
set EventFile to "data/ratings-book.csv"
bind EventFormat to Formats.csvRatings()