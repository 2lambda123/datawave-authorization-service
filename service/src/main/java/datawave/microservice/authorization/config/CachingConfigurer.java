package datawave.microservice.authorization.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Spring caching support to supply a special error handler. The error handler clears the entire cache if a get request fails. This is done since
 * the main reason a get request would fail is if the cache used a backing store and the entries in the backing store can no longer be loaded.
 */
@Configuration
@EnableCaching
public class CachingConfigurer extends CachingConfigurerSupport {
    
    @Bean
    @ConditionalOnProperty(prefix = "hazelcast", name = "error.handler.clearOnGet", havingValue = "true", matchIfMissing = true)
    @Override
    public CacheErrorHandler errorHandler() {
        return new ClearOnGetFailureHandler();
    }
    
    private static class ClearOnGetFailureHandler implements CacheErrorHandler {
        private final Logger logger = LoggerFactory.getLogger(getClass());
        
        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            // do nothing -- we want it treated as a cache miss
            logger.warn("Exception retrieving value for {} from cache {}", key, exception.getMessage(), exception);
            try {
                cache.clear();
                logger.warn("Cleared cache due to retrieval errors.");
            } catch (Exception e) {
                logger.error("Error clearing cache {} after get error on {}: {}", cache.getName(), key, exception.getMessage(), exception);
            }
        }
        
        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            logger.warn("Error putting value in {}. key={} -> value={}: {}", cache.getName(), key, value, exception.getMessage(), exception);
        }
        
        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            logger.warn("Error evicting value from {}. key={}: {}", cache.getName(), key, exception.getMessage(), exception);
        }
        
        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            logger.error("Error clearing cache {}: {}", cache.getName(), exception.getMessage(), exception);
        }
    }
}
