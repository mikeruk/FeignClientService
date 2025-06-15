package dating.configExcludedFromComponentScan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {
    /**
     * e.g. "v1", "v2", etc.
     */
    String value();
}
