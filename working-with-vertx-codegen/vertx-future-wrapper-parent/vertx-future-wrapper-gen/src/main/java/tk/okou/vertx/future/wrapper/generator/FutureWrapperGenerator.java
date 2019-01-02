package tk.okou.vertx.future.wrapper.generator;

import io.vertx.codegen.ClassModel;
import io.vertx.codegen.Generator;

import java.util.Collections;
import java.util.Map;

class FutureWrapperGenerator extends Generator<ClassModel> {
  FutureWrapperGenerator() {
    this.name = "FutureWrapper";
    this.kinds = Collections.singleton("class");
  }

  @Override
  public String filename(ClassModel model) {
    return model.getType().translateName("future") + ".java";
  }

  @Override
  public String render(ClassModel model, int index, int size, Map<String, Object> session) {
    return super.render(model, index, size, session);
  }
}
