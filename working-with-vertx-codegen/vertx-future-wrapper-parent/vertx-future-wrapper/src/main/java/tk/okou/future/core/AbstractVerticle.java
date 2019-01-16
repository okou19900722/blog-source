package tk.okou.future.core;

import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import io.vertx.future.core.Context;
import io.vertx.future.core.Vertx;

import java.util.List;

public abstract class AbstractVerticle implements Verticle {

  /**
   * Reference to the Vert.x instance that deployed this verticle
   */
  protected Vertx vertx;

  /**
   * Reference to the context of the verticle
   */
  protected Context context;

  /**
   * Get the Vert.x instance
   *
   * @return the Vert.x instance
   */
  @Override
  public io.vertx.core.Vertx getVertx() {
    return vertx.getDelegate();
  }

  /**
   * Initialise the verticle.<p>
   * This is called by Vert.x when the verticle instance is deployed. Don't call it yourself.
   *
   * @param vertx   the deploying Vert.x instance
   * @param context the context of the verticle
   */
  @Override
  public void init(io.vertx.core.Vertx vertx, io.vertx.core.Context context) {
    this.vertx = Vertx.newInstance(vertx);
    this.context = Context.newInstance(context);
  }

  /**
   * Get the deployment ID of the verticle deployment
   *
   * @return the deployment ID
   */
  public String deploymentID() {
    return context.deploymentID();
  }

  /**
   * Get the configuration of the verticle.
   * <p>
   * This can be specified when the verticle is deployed.
   *
   * @return the configuration
   */
  public JsonObject config() {
    return context.config();
  }

  /**
   * Get the arguments used when deploying the Vert.x process.
   *
   * @return the list of arguments
   */
  public List<String> processArgs() {
    return context.processArgs();
  }

  /**
   * Start the verticle.<p>
   * This is called by Vert.x when the verticle instance is deployed. Don't call it yourself.<p>
   * If your verticle does things in its startup which take some time then you can override this method
   * and call the startFuture some time later when start up is complete.
   *
   * @param startFuture a future which should be called when verticle start-up is complete.
   * @throws Exception
   */
  @Override
  public void start(io.vertx.core.Future<Void> startFuture) throws Exception {
    start();
    startFuture.complete();
  }

  /**
   * Stop the verticle.<p>
   * This is called by Vert.x when the verticle instance is un-deployed. Don't call it yourself.<p>
   * If your verticle does things in its shut-down which take some time then you can override this method
   * and call the stopFuture some time later when clean-up is complete.
   *
   * @param stopFuture a future which should be called when verticle clean-up is complete.
   * @throws Exception
   */
  @Override
  public void stop(io.vertx.core.Future<Void> stopFuture) throws Exception {
    stop();
    stopFuture.complete();
  }

  /**
   * If your verticle does a simple, synchronous start-up then override this method and put your start-up
   * code in here.
   *
   * @throws Exception
   */
  public void start() throws Exception {
  }

  /**
   * If your verticle has simple synchronous clean-up tasks to complete then override this method and put your clean-up
   * code in here.
   *
   * @throws Exception
   */
  public void stop() throws Exception {
  }

}
