package de.telekom.spica.framework.swagger.annotation;

/**
 *
 * @author Robert Winkler
 *
 */

import de.telekom.spica.framework.swagger.config.SwaggerConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * This annotation should be applied to a Spring Boot Admin application to enable Swagger API documentation.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@Documented
@Import(SwaggerConfig.class)
public @interface EnableAdminServerSwagger {
}
