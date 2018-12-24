package examples;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

@ProxyGen
public interface FooService {

    void foo(String foo, Handler<AsyncResult<Void>> result);

}