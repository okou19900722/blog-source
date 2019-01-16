package tk.okou.vertx.future.wrapper;

public class Helper {
  public Helper() {
  }

  public static Class unwrap(Class<?> type) {
    if (type != null) {
      FutureGen futureGen = type.getAnnotation(FutureGen.class);
      if (futureGen != null) {
        return futureGen.value();
      }
    }

    return type;
  }
}
