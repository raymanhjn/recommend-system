package recsys.dao;

import org.lenskit.inject.Parameter;

import javax.inject.Qualifier;
import java.io.File;
import java.lang.annotation.*;

/**
 * Parameter annotation for the book tag file.
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Qualifier
@Parameter(File.class)
public @interface BookFile {
}
