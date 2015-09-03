package app;

import app.web.AsyncTestController;
import app.web.interceptor.TestInterceptor;
import core.framework.api.App;
import core.framework.api.module.SystemModule;

/**
 * @author neo
 */
public class DemoServiceApp extends App {
    @Override
    protected void initialize() {
        load(new SystemModule("sys.properties"));

        http().intercept(bind(TestInterceptor.class));
        route().get("/async-test", bind(AsyncTestController.class));

        load(new ProductModule());
        load(new UserModule());
//        load(new JobModule());
    }
}
