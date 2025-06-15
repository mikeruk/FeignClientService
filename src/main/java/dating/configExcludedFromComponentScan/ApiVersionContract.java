package dating.configExcludedFromComponentScan;

import feign.Contract;
import feign.MethodMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Custom Contract that applies @ApiVersion at class and method level
 * by rewriting only the path (not the full target URL) and adding a header.
 */
public class ApiVersionContract extends SpringMvcContract {

    private final String apiKeyValue;

    public ApiVersionContract(String apiKeyValue) {
        super();
        this.apiKeyValue = apiKeyValue;
    }


    @Override
    protected void processAnnotationOnClass(MethodMetadata data, Class<?> targetType) {
        // 1) let SpringMvcContract handle @RequestMapping at class level
        super.processAnnotationOnClass(data, targetType);

        // 2) then apply our @ApiVersion if present
        ApiVersion apiVer = targetType.getAnnotation(ApiVersion.class);
        if (apiVer != null) {
            String versionPrefix = "/" + apiVer.value();
            // set base path
            data.template().uri(versionPrefix);
            data.template().header("X-API-Version", apiVer.value());
        }
    }

    @Override
    protected void processAnnotationOnMethod(MethodMetadata data,
                                             Annotation ann,
                                             Method method) {
        super.processAnnotationOnMethod(data, ann, method);

        ApiVersion ver = method.getAnnotation(ApiVersion.class);
        if (ver != null) {
            String version = ver.value().startsWith("/")
                    ? ver.value()
                    : "/" + ver.value();
            String existing = data.template().url();  // e.g. "/user/{id}"
            // only prefix if it isn’t already there
            if (!existing.startsWith(version)) {
                data.template().uri(version + existing, false);
                data.template().header("X-API-Version", ver.value());
            }
        }
    }

    @Override
    protected boolean processAnnotationsOnParameter(MethodMetadata data,
                                                    Annotation[] annotations,
                                                    int paramIndex) {
        // no changes to parameter handling
        return super.processAnnotationsOnParameter(data, annotations, paramIndex);
    }



    @Override
    public List<MethodMetadata> parseAndValidateMetadata(Class<?> targetType) {
        // 1) let SpringMvcContract build all the normal metadata
        List<MethodMetadata> list = super.parseAndValidateMetadata(targetType);

        // 2) then for each method, add '?apiKey=...' to its template
        for (MethodMetadata md : list) {
            md.template().query("apiKey", apiKeyValue);
        }
        return list;
    }

}
