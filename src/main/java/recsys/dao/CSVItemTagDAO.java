package recsys.dao;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.apache.commons.lang3.text.StrTokenizer;
import org.lenskit.data.dao.DataAccessException;
import org.lenskit.util.io.LineStream;
import org.lenskit.util.io.ObjectStream;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public class CSVItemTagDAO implements ItemTagDAO {
    private final File tagFile;
    private transient volatile Long2ObjectMap<List<String>> tagCache;
    private transient volatile Set<String> vocabCache;

    @Inject
    public CSVItemTagDAO(@TagFile File tags) {
        tagFile = tags;
    }

    private void ensureTagCache() {
        if (tagCache == null) {
            synchronized (this) {
                if (tagCache == null) {
                    tagCache = new Long2ObjectOpenHashMap<List<String>>();
                    ImmutableSet.Builder<String> vocabBuilder = ImmutableSet.builder();
                    ObjectStream<List<String>> lines = null;
                    try {
                        LineStream stream = LineStream.openFile(tagFile);
                        lines = stream.tokenize(StrTokenizer.getCSVInstance());
                    } catch (FileNotFoundException e) {
                        throw new DataAccessException("cannot open file", e);
                    }
                    try {
                        for (List<String> line: lines) {
                            long mid = Long.parseLong(line.get(0));
                            List<String> tags = tagCache.get(mid);
                            if (tags == null) {
                                tags = new ArrayList<String>();
                                tagCache.put(mid, tags);
                            }
                            tags.add(line.get(1));
                            vocabBuilder.add(line.get(1));
                        }
                    } finally {
                        lines.close();
                    }
                    vocabCache = vocabBuilder.build();
                }
            }
        }
    }

    @Override
    public List<String> getItemTags(long item) {
        ensureTagCache();
        List<String> tags = tagCache.get(item);
        if (tags != null) {
            return Collections.unmodifiableList(tags);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Set<String> getTagVocabulary() {
        ensureTagCache();
        return vocabCache;
    }
}
