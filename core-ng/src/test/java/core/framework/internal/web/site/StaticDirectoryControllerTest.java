package core.framework.internal.web.site;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author neo
 */
class StaticDirectoryControllerTest {
    private StaticDirectoryController controller;

    @BeforeEach
    void createStaticDirectoryController() {
        controller = new StaticDirectoryController(null);
    }

    @Test
    void cache() {
        controller.cache(Duration.ofMinutes(10));

        assertEquals("public, max-age=600", controller.cacheHeader);
    }
}
