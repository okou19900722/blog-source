package tk.okou.vertx.future.wrapper.generator;

import io.vertx.codegen.*;
import io.vertx.codegen.annotations.ModuleGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.codegen.doc.Doc;
import io.vertx.codegen.doc.Tag;
import io.vertx.codegen.doc.Token;
import io.vertx.codegen.type.*;
import io.vertx.codegen.writer.CodeWriter;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import tk.okou.vertx.future.wrapper.FutureGen;

import javax.lang.model.element.Element;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

import static io.vertx.codegen.type.ClassKind.*;

class FutureWrapperGenerator extends Generator<ClassModel> {
  FutureWrapperGenerator() {
    this.name = "FutureWrapper";
    this.kinds = Collections.singleton("class");
  }

  @Override
  public Collection<Class<? extends Annotation>> annotations() {
    return Arrays.asList(VertxGen.class, ModuleGen.class);
  }

  private boolean isFuture(ClassTypeInfo typeInfo) {
    String name = typeInfo.getRaw().getName();
    return name.equals(Future.class.getName()) || name.equals(CompositeFuture.class.getName());
  }

  @Override
  public String filename(ClassModel model) {
    if (isFuture(model.getType())) {
      return null;
    }
    return model.getType().translateName("future") + ".java";
  }

  @Override
  public String render(ClassModel model, int index, int size, Map<String, Object> session) {
    ClassTypeInfo type = model.getType();
    StringWriter sb = new StringWriter();
    CodeWriter writer = new CodeWriter(sb);

    generateLicense(writer);

    writer.print("package ");
    writer.print(type.translatePackageName("future"));
    writer.println(";");
    writer.println();

    writer.println("import java.util.Map;");
    genFutureImports(model, writer);

    writer.println();
    generateDoc(model, writer);
    writer.println();

    writer.format("@%s(%s.class)", FutureGen.class.getName(), type.getName()).println();

    writer.print("public ");
    if (model.isConcrete()) {
      writer.print("class");
    } else {
      writer.print("interface");
    }
    writer.print(" ");
    writer.print(Helper.getSimpleName(model.getIfaceFQCN()));

    if ("io.vertx.core.buffer.Buffer".equals(type.getName())) {
      writer.print(" implements io.vertx.core.shareddata.impl.ClusterSerializable");
    }
    if (model.isConcrete() && model.getConcreteSuperType() != null) {
      writer.print(" extends ");
      writer.print(genTypeName(model.getConcreteSuperType()));
    }
    List<TypeInfo> abstractSuperTypes = model.getAbstractSuperTypes();
    if (abstractSuperTypes.size() > 0) {
      writer.print(" ");
      if (model.isConcrete()) {
        writer.print("implements");
      } else {
        writer.print("extends");
      }
      writer.print(abstractSuperTypes.stream().map(it -> " " + genTypeName(it)).collect(Collectors.joining(", ")));
    }
    TypeInfo handlerType = model.getHandlerType();
    if (handlerType != null) {
      if (abstractSuperTypes.isEmpty()) {
        writer.print(" ");
        if (model.isConcrete()) {
          writer.print("implements ");
        } else {
          writer.print("extends ");
        }
      } else {
        writer.print(", ");
      }
      writer.print("io.vertx.core.Handler<");
      writer.print(genTypeName(handlerType));
      writer.print(">");
    }
    writer.println(" {");
    writer.println();

    if (model.isConcrete()) {
      if ("io.vertx.core.buffer.Buffer".equals(type.getName())) {
        writer.println("  @Override");
        writer.println("  public void writeToBuffer(io.vertx.core.buffer.Buffer buffer) {");
        writer.println("    delegate.writeToBuffer(buffer);");
        writer.println("  }");
        writer.println();
        writer.println("  @Override");
        writer.println("  public int readFromBuffer(int pos, io.vertx.core.buffer.Buffer buffer) {");
        writer.println("    return delegate.readFromBuffer(pos, buffer);");
        writer.println("  }");
        writer.println();
      }

      List<MethodInfo> methods = model.getMethods();
      if (methods.stream().noneMatch(it -> it.getParams().isEmpty() && "toString".equals(it.getName()))) {
        writer.println("  @Override");
        writer.println("  public String toString() {");
        writer.println("    return delegate.toString();");
        writer.println("  }");
        writer.println();
      }

      writer.println("  @Override");
      writer.println("  public boolean equals(Object o) {");
      writer.println("    if (this == o) return true;");
      writer.println("    if (o == null || getClass() != o.getClass()) return false;");
      writer.print("    ");
      writer.print(type.getSimpleName());
      writer.print(" that = (");
      writer.print(type.getSimpleName());
      writer.println(") o;");
      writer.println("    return delegate.equals(that.delegate);");
      writer.println("  }");
      writer.println("  ");

      writer.println("  @Override");
      writer.println("  public int hashCode() {");
      writer.println("    return delegate.hashCode();");
      writer.println("  }");
      writer.println();

      generateClassBody(model, model.getIfaceSimpleName(), writer);
    } else {
      writer.print("  ");
      writer.print(type.getName());
      writer.println(" getDelegate();");
      writer.println();

      for (MethodInfo method : model.getMethods()) {
        startMethodTemplate(method, writer);
        writer.println(";");
        writer.println();
      }
    }
    writer.println();
    writer.print("  public static ");
    writer.print(genOptTypeParamsDecl(type, " "));
    writer.print(type.getSimpleName());
    writer.print(genOptTypeParamsDecl(type, ""));
    writer.print(" newInstance(");
    writer.print(type.getName());
    writer.println(" arg) {");

    writer.print("    return arg != null ? new ");
    writer.print(type.getSimpleName());
    if (!model.isConcrete()) {
      writer.print("Impl");
    }
    writer.print(genOptTypeParamsDecl(type, ""));
    writer.println("(arg) : null;");
    writer.println("  }");

    if (type.getParams().size() > 0) {
      writer.println();
      writer.print("  public static ");
      writer.print(genOptTypeParamsDecl(type, " "));
      writer.print(type.getSimpleName());
      writer.print(genOptTypeParamsDecl(type, ""));
      writer.print(" newInstance(");
      writer.print(type.getName());
      writer.print(" arg");
      for (TypeParamInfo typeParam : type.getParams()) {
        writer.print(", tk.okou.vertx.future.wrapper.TypeArg<");
        writer.print(typeParam.getName());
        writer.print("> __typeArg_");
        writer.print(typeParam.getName());
      }
      writer.println(") {");

      writer.print("    return arg != null ? new ");
      writer.print(type.getSimpleName());
      if (!model.isConcrete()) {
        writer.print("Impl");
      }
      writer.print(genOptTypeParamsDecl(type, ""));
      writer.print("(arg");
      for (TypeParamInfo typeParam : type.getParams()) {
        writer.print(", __typeArg_");
        writer.print(typeParam.getName());
      }
      writer.println(") : null;");
      writer.println("  }");
    }
    writer.println("}");

    if (!model.isConcrete()) {
      writer.println();
      writer.print("class ");
      writer.print(type.getSimpleName());
      writer.print("Impl");
      writer.print(genOptTypeParamsDecl(type, ""));
      writer.print(" implements ");
      writer.print(Helper.getSimpleName(model.getIfaceFQCN()));
      writer.println(" {");
      generateClassBody(model, type.getSimpleName() + "Impl", writer);
      writer.println("}");
    }
    return sb.toString();
  }

  private void generateClassBody(ClassModel model, String constructor, PrintWriter writer) {
    ClassTypeInfo type = model.getType();
    String simpleName = type.getSimpleName();
    if (model.isConcrete()) {
      writer.print("  public static final tk.okou.vertx.future.wrapper.TypeArg<");
      writer.print(simpleName);
      writer.print("> __TYPE_ARG = new tk.okou.vertx.future.wrapper.TypeArg<>(");
      writer.print("    obj -> new ");
      writer.print(simpleName);
      writer.print("((");
      writer.print(type.getName());
      writer.println(") obj),");
      writer.print("    ");
      writer.print(simpleName);
      writer.println("::getDelegate");
      writer.println("  );");
      writer.println();
    }
    writer.print("  private final ");
    writer.print(Helper.getNonGenericType(model.getIfaceFQCN()));
    List<TypeParamInfo.Class> typeParams = model.getTypeParams();
    if (typeParams.size() > 0) {
      writer.print(typeParams.stream().map(TypeParamInfo.Class::getName).collect(Collectors.joining(",", "<", ">")));
    }
    writer.println(" delegate;");

    for (TypeParamInfo.Class typeParam : typeParams) {
      writer.print("  public final tk.okou.vertx.future.wrapper.TypeArg<");
      writer.print(typeParam.getName());
      writer.print("> __typeArg_");
      writer.print(typeParam.getIndex());
      writer.println(";");
    }
    writer.println("  ");

    writer.print("  public ");
    writer.print(constructor);
    writer.print("(");
    writer.print(Helper.getNonGenericType(model.getIfaceFQCN()));
    writer.println(" delegate) {");

    if (model.isConcrete() && model.getConcreteSuperType() != null) {
      writer.println("    super(delegate);");
    }
    writer.println("    this.delegate = delegate;");
    for (TypeParamInfo.Class typeParam : typeParams) {
      writer.print("    this.__typeArg_");
      writer.print(typeParam.getIndex());
      writer.print(" = tk.okou.vertx.future.wrapper.TypeArg.unknown();");
    }
    writer.println("  }");
    writer.println();

    if (typeParams.size() > 0) {
      writer.print("  public ");
      writer.print(constructor);
      writer.print("(");
      writer.print(Helper.getNonGenericType(model.getIfaceFQCN()));
      writer.print(" delegate");
      for (TypeParamInfo.Class typeParam : typeParams) {
        writer.print(", tk.okou.vertx.future.wrapper.TypeArg<");
        writer.print(typeParam.getName());
        writer.print("> typeArg_");
        writer.print(typeParam.getIndex());
      }
      writer.println(") {");
      if (model.isConcrete() && model.getConcreteSuperType() != null) {
        writer.println("    super(delegate);");
      }
      writer.println("    this.delegate = delegate;");
      for (TypeParamInfo.Class typeParam : typeParams) {
        writer.print("    this.__typeArg_");
        writer.print(typeParam.getIndex());
        writer.print(" = typeArg_");
        writer.print(typeParam.getIndex());
        writer.println(";");
      }
      writer.println("  }");
      writer.println();
    }

    writer.print("  public ");
    writer.print(type.getName());
    writer.println(" getDelegate() {");
    writer.println("    return delegate;");
    writer.println("  }");
    writer.println();

    List<String> cacheDecls = new ArrayList<>();
    for (MethodInfo method : model.getMethods()) {
      genMethods(model, method, cacheDecls, writer);
    }
    for (MethodInfo method : model.getAnyJavaTypeMethods()) {
      genMethods(model, method, cacheDecls, writer);
    }

    for (ConstantInfo constant : model.getConstants()) {
      genConstant(model, constant, writer);
    }

    for (String cacheDecl : cacheDecls) {
      writer.print("  ");
      writer.print(cacheDecl);
      writer.println(";");
    }
  }


  private void genConstant(ClassModel model, ConstantInfo constant, PrintWriter writer) {
    Doc doc = constant.getDoc();
    if (doc != null) {
      writer.println("  /**");
      Token.toHtml(doc.getTokens(), "   *", this::renderLinkToHtml, "\n", writer);
      writer.println("   */");
    }
    writer.print(model.isConcrete() ? "  public static final" : "");
    writer.println(" " + constant.getType().getSimpleName() + " " + constant.getName() + " = "
      + genConvReturn(constant.getType(), null, model.getType().getName() + "." + constant.getName()) + ";");
  }

  private void genMethods(ClassModel model, MethodInfo method, List<String> cacheDecls, PrintWriter writer) {
    this.genMethod(model, method, cacheDecls, writer);
  }


  private String genFutureMethodName(MethodInfo method) {
    return "async" + Character.toUpperCase(method.getName().charAt(0)) + method.getName().substring(1);
  }

  private MethodInfo genFutureMethod(MethodInfo method) {
    String futMethodName = this.genFutureMethodName(method);
    List<ParamInfo> futParams = new ArrayList<>();
    int count = 0;

    int size;
    ParamInfo futParam;
    for (size = method.getParams().size() - 1; count < size; ++count) {
      futParam = method.getParam(count);
      futParams.add(futParam);
    }

    futParam = method.getParam(size);
    TypeInfo futType = ((ParameterizedTypeInfo) ((ParameterizedTypeInfo) futParam.getType()).getArg(0)).getArg(0);
    TypeInfo futUnresolvedType = ((ParameterizedTypeInfo) ((ParameterizedTypeInfo) futParam.getUnresolvedType()).getArg(0)).getArg(0);
    TypeInfo futReturnType;
    if (futUnresolvedType.getKind() == ClassKind.VOID) {
      futType = TypeReflectionFactory.create(Void.class);
      futReturnType = new ParameterizedTypeInfo(TypeReflectionFactory.create(Future.class).getRaw(), false, Collections.singletonList(futType));
    } else if (futUnresolvedType.isNullable()) {
      futReturnType = new ParameterizedTypeInfo(TypeReflectionFactory.create(Future.class).getRaw(), false, Collections.singletonList(futType));
    } else {
      futReturnType = new ParameterizedTypeInfo(TypeReflectionFactory.create(Future.class).getRaw(), false, Collections.singletonList(futType));
    }

    return method.copy().setName(futMethodName).setReturnType(futReturnType).setParams(futParams);
  }

  private void genRxMethod(MethodInfo method, PrintWriter writer) {
    MethodInfo futMethod = this.genFutureMethod(method);
    String adapterType = "io.vertx.core.Future.future";
    this.startMethodTemplate(futMethod, writer);
    writer.println(" { ");
    writer.print("    return ");
    writer.print(adapterType);
    writer.println("(handler -> {");
    writer.print("      ");
    writer.print(method.getName());
    writer.print("(");
    List<ParamInfo> params = futMethod.getParams();
    writer.print(params.stream().map(ParamInfo::getName).collect(Collectors.joining(", ")));
    if (params.size() > 0) {
      writer.print(", ");
    }

    writer.println("handler);");
    writer.println("    });");
    writer.println("  }");
    writer.println();
  }

  private void genMethod(ClassModel model, MethodInfo method, List<String> cacheDecls, PrintWriter writer) {
    genSimpleMethod(model, method, cacheDecls, writer);
    if (method.getKind() == MethodKind.FUTURE) {
      genRxMethod(method, writer);
    }
  }

  private void genSimpleMethod(ClassModel model, MethodInfo method, List<String> cacheDecls, PrintWriter writer) {
    startMethodTemplate(method, writer);
    writer.println(" { ");
    if (method.isFluent()) {
      writer.print("    ");
      writer.print(genInvokeDelegate(model, method));
      writer.println(";");
      if (method.getReturnType().isVariable()) {
        writer.print("    return (");
        writer.print(method.getReturnType().getName());
        writer.println(") this;");
      } else {
        writer.println("    return this;");
      }
    } else if (method.getReturnType().getName().equals("void")) {
      writer.print("    ");
      writer.print(genInvokeDelegate(model, method));
      writer.println(";");
    } else {
      if (method.isCacheReturn()) {
        writer.print("    if (cached_");
        writer.print(cacheDecls.size());
        writer.println(" != null) {");

        writer.print("      return cached_");
        writer.print(cacheDecls.size());
        writer.println(";");
        writer.println("    }");
      }
      String cachedType;
      TypeInfo returnType = method.getReturnType();
      if (method.getReturnType().getKind() == PRIMITIVE) {
        cachedType = ((PrimitiveTypeInfo) returnType).getBoxed().getName();
      } else {
        cachedType = genTypeName(returnType);
      }
      writer.print("    ");
      writer.print(genTypeName(returnType));
      writer.print(" ret = ");
      writer.print(genConvReturn(returnType, method, genInvokeDelegate(model, method)));
      writer.println(";");
      if (method.isCacheReturn()) {
        writer.print("    cached_");
        writer.print(cacheDecls.size());
        writer.println(" = ret;");
        cacheDecls.add("private" + (method.isStaticMethod() ? " static" : "") + " " + cachedType + " cached_" + cacheDecls.size());
      }
      writer.println("    return ret;");
    }
    writer.println("  }");
    writer.println();
  }

  private String genInvokeDelegate(ClassModel model, MethodInfo method) {
    StringBuilder ret;
    if (method.isStaticMethod()) {
      ret = new StringBuilder(Helper.getNonGenericType(model.getIfaceFQCN()));
    } else {
      ret = new StringBuilder("delegate");
    }
    ret.append(".").append(method.getName()).append("(");
    int index = 0;
    for (ParamInfo param : method.getParams()) {
      if (index > 0) {
        ret.append(", ");
      }
      TypeInfo type = param.getType();
      ret.append(genConvParam(type, method, param.getName()));
      index = index + 1;
    }
    ret.append(")");
    return ret.toString();
  }

  private String genConvParam(TypeInfo type, MethodInfo method, String expr) {
    ClassKind kind = type.getKind();
    if (isSameType(type, method)) {
      return expr;
    } else if (kind == OBJECT) {
      if (type.isVariable()) {
        String typeArg = genTypeArg((TypeVariableInfo) type, method);
        if (typeArg != null) {
          return typeArg + ".<" + type.getName() + ">unwrap(" + expr + ")";
        }
      }
      return expr;
    } else if (kind == API) {
      if (isFuture(type.getRaw())) {
        if (type instanceof ParameterizedTypeInfo) {
          ParameterizedTypeInfo api = (ParameterizedTypeInfo) type;
          return expr + ".map(" + api.getArg(0).translateName("future") + "::getDelegate)";
        } else {
          ApiTypeInfo api = (ApiTypeInfo) type;
          return expr + "/*A:" + api.getHandlerArg().getName() + "*/";
        }
      } else {
        return expr + ".getDelegate()";
      }
    } else if (kind == CLASS_TYPE) {
      return tk.okou.vertx.future.wrapper.Helper.class.getName() + ".unwrap(" + expr + ")";
    } else if (type.isParameterized()) {
      ParameterizedTypeInfo parameterizedTypeInfo = (ParameterizedTypeInfo) type;
      if (kind == HANDLER) {
        TypeInfo eventType = parameterizedTypeInfo.getArg(0);
        ClassKind eventKind = eventType.getKind();
        if (eventKind == ASYNC_RESULT) {
          TypeInfo resultType = ((ParameterizedTypeInfo) eventType).getArg(0);
          return "new Handler<AsyncResult<" + resultType.getName() + ">>() {\n" +
            "      public void handle(AsyncResult<" + resultType.getName() + "> ar) {\n" +
            "        if (ar.succeeded()) {\n" +
            "          " + expr + ".handle(io.vertx.core.Future.succeededFuture(" + genConvReturn(resultType, method, "ar.result()") + "));\n" +
            "        } else {\n" +
            "          " + expr + ".handle(io.vertx.core.Future.failedFuture(ar.cause()));\n" +
            "        }\n" +
            "      }\n" +
            "    }";
        } else {
          return "new Handler<" + eventType.getName() + ">() {\n" +
            "      public void handle(" + eventType.getName() + " event) {\n" +
            "        " + expr + ".handle(" + genConvReturn(eventType, method, "event") + ");\n" +
            "      }\n" +
            "    }";
        }
      } else if (kind == FUNCTION) {
        TypeInfo argType = parameterizedTypeInfo.getArg(0);
        TypeInfo retType = parameterizedTypeInfo.getArg(1);
        return " arg -> {\n" +
          "        " + genTypeName(retType) + " ret = " + expr + ".apply(" + genConvReturn(argType, method, "arg") + ");\n" +
          "        return " + genConvParam(retType, method, "ret") + ";\n" +
          "    }";
      } else if (kind == LIST || kind == SET) {
        return expr + ".stream().map(elt -> " + genConvParam(parameterizedTypeInfo.getArg(0), method, "elt") + ").collect(java.util.stream.Collectors.to" + type.getRaw().getSimpleName() + "())";
      } else if (kind == MAP) {
        return expr + ".entrySet().stream().collect(java.util.stream.Collectors.toMap(e -> e.getKey(), e -> " + genConvParam(parameterizedTypeInfo.getArg(1), method, "e.getValue()") + "))";
      }
    }
    return expr;
  }

  private String genConvReturn(TypeInfo type, MethodInfo method, String expr) {
    ClassKind kind = type.getKind();
    if (kind == OBJECT) {
      if (type.isVariable()) {
        String typeArg = genTypeArg((TypeVariableInfo) type, method);
        if (typeArg != null) {
          return "(" + type.getName() + ")" + typeArg + ".wrap(" + expr + ")";
        }
      }
      return "(" + type.getSimpleName() + ") " + expr;
    } else if (isSameType(type, method)) {
      return expr;
    } else if (kind == API) {
      if (isFuture(type.getRaw())) {
        return expr;
      }
      StringBuilder tmp = new StringBuilder(type.getRaw().translateName("future"));
      tmp.append(".newInstance(");
      tmp.append(expr);
      if (type.isParameterized()) {
        ParameterizedTypeInfo parameterizedTypeInfo = (ParameterizedTypeInfo) type;
        for (TypeInfo arg : parameterizedTypeInfo.getArgs()) {
          tmp.append(", ");
          ClassKind argKind = arg.getKind();
          if (argKind == API) {
            tmp.append(arg.translateName("future")).append(".__TYPE_ARG");
          } else {
            String typeArg = "tk.okou.vertx.future.wrapper.TypeArg.unknown()";
            if (argKind == OBJECT && arg.isVariable()) {
              String resolved = genTypeArg((TypeVariableInfo) arg, method);
              if (resolved != null) {
                typeArg = resolved;
              }
            }
            tmp.append(typeArg);
          }
        }
      }
      tmp.append(")");
      return tmp.toString();
    } else if (type.isParameterized()) {
      ParameterizedTypeInfo parameterizedTypeInfo = (ParameterizedTypeInfo) type;
      if (kind == HANDLER) {
        TypeInfo abc = parameterizedTypeInfo.getArg(0);
        if (abc.getKind() == ASYNC_RESULT) {
          TypeInfo tutu = ((ParameterizedTypeInfo) abc).getArg(0);
          return "new Handler<AsyncResult<" + genTypeName(tutu) + ">>() {\n" +
            "      public void handle(AsyncResult<" + genTypeName(tutu) + "> ar) {\n" +
            "        if (ar.succeeded()) {\n" +
            "          " + expr + ".handle(io.vertx.core.Future.succeededFuture(" + genConvParam(tutu, method, "ar.result()") + "));\n" +
            "        } else {\n" +
            "          " + expr + ".handle(io.vertx.core.Future.failedFuture(ar.cause()));\n" +
            "        }\n" +
            "      }\n" +
            "    }";
        } else {
          return "new Handler<" + genTypeName(abc) + ">() {\n" +
            "      public void handle(" + genTypeName(abc) + " event) {\n" +
            "          " + expr + ".handle(" + genConvParam(abc, method, "event") + ");\n" +
            "      }\n" +
            "    }";
        }
      } else if (kind == LIST || kind == SET) {
        return expr + ".stream().map(elt -> " + genConvReturn(parameterizedTypeInfo.getArg(0), method, "elt") + ").collect(java.util.stream.Collectors.to" + type.getRaw().getSimpleName() + "())";
      }
    }
    return expr;
  }

  private boolean isSameType(TypeInfo type, MethodInfo method) {
    ClassKind kind = type.getKind();
    if (kind.basic || kind.json || kind == DATA_OBJECT || kind == ENUM || kind == OTHER || kind == THROWABLE || kind == VOID) {
      return true;
    } else if (kind == OBJECT) {
      if (type.isVariable()) {
        return !isReified((TypeVariableInfo) type, method);
      } else {
        return true;
      }
    } else if (type.isParameterized()) {
      ParameterizedTypeInfo parameterizedTypeInfo = (ParameterizedTypeInfo) type;
      if (kind == LIST || kind == SET || kind == ASYNC_RESULT) {
        return isSameType(parameterizedTypeInfo.getArg(0), method);
      } else if (kind == MAP) {
        return isSameType(parameterizedTypeInfo.getArg(1), method);
      } else if (kind == HANDLER) {
        return isSameType(parameterizedTypeInfo.getArg(0), method);
      } else if (kind == FUNCTION) {
        return isSameType(parameterizedTypeInfo.getArg(0), method) && isSameType(parameterizedTypeInfo.getArg(1), method);
      }
    }
    return false;
  }

  private boolean isReified(TypeVariableInfo typeVar, MethodInfo method) {
    if (typeVar.isClassParam()) {
      return true;
    } else {
      TypeArgExpression typeArg = method.resolveTypeArg(typeVar);
      return typeArg != null && typeArg.isClassType();
    }
  }

  private String genTypeArg(TypeVariableInfo typeVar, MethodInfo method) {
    if (typeVar.isClassParam()) {
      return "__typeArg_" + typeVar.getParam().getIndex();
    } else {
      TypeArgExpression typeArg = method.resolveTypeArg(typeVar);
      if (typeArg != null) {
        if (typeArg.isClassType()) {
          return "tk.okou.vertx.future.wrapper.TypeArg.of(" + typeArg.getParam().getName() + ")";
        } else {
          return typeArg.getParam().getName() + ".__typeArg_" + typeArg.getIndex();
        }
      }
    }
    return null;
  }

  private String genTypeName(TypeInfo type) {
    if (type.isParameterized()) {
      ParameterizedTypeInfo pt = (ParameterizedTypeInfo) type;
      return genTypeName(pt.getRaw()) + pt.getArgs().stream().map(this::genTypeName).collect(Collectors.joining(", ", "<", ">"));
    } else if (type.getKind() == ClassKind.API) {
      if (isFuture(type.getRaw())) {
        return type.getRaw().getName();
      } else {
        return type.translateName("future");
      }
    } else {
      return type.getSimpleName();
    }
  }


  private void generateDoc(ClassModel model, PrintWriter writer) {
    ClassTypeInfo type = model.getType();
    Doc doc = model.getDoc();
    if (doc != null) {
      writer.println("/**");
      Token.toHtml(doc.getTokens(), " *", this::renderLinkToHtml, "\n", writer);
      writer.println(" *");
      writer.println(" * <p/>");
      writer.print(" * NOTE: This class has been automatically generated from the {@link ");
      writer.print(type.getName());
      writer.println(" original} non RX-ified interface using Vert.x codegen.");
      writer.println(" */");
    }
  }


  private String renderLinkToHtml(Tag.Link link) {
    ClassTypeInfo rawType = link.getTargetType().getRaw();
    if (rawType.getModule() != null) {
      String label = link.getLabel().trim();
      if (rawType.getKind() == DATA_OBJECT) {
        return "{@link " + rawType.getName() + "}";
      } else {
        if (rawType.getKind() == ClassKind.API) {
          Element elt = link.getTargetElement();
          String eltKind = elt.getKind().name();
          String ret = "{@link " + rawType.translateName("future");
          if ("METHOD".equals(eltKind)) {
            /* todo find a way for translating the complete signature */
            ret += "#" + elt.getSimpleName().toString();
          }
          if (label.length() > 0) {
            ret += " " + label;
          }
          ret += "}";
          return ret;
        }
      }
    }
    return "{@link " + rawType.getName() + "}";
  }


  private void generateLicense(PrintWriter writer) {
    writer.println("/*");
    writer.println(" * Copyright 2014 Red Hat, Inc.");
    writer.println(" *");
    writer.println(" * Red Hat licenses this file to you under the Apache License, version 2.0");
    writer.println(" * (the \"License\"); you may not use this file except in compliance with the");
    writer.println(" * License.  You may obtain a copy of the License at:");
    writer.println(" *");
    writer.println(" * http://www.apache.org/licenses/LICENSE-2.0");
    writer.println(" *");
    writer.println(" * Unless required by applicable law or agreed to in writing, software");
    writer.println(" * distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT");
    writer.println(" * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the");
    writer.println(" * License for the specific language governing permissions and limitations");
    writer.println(" * under the License.");
    writer.println(" */");
    writer.println();
  }

  private void genFutureImports(ClassModel model, PrintWriter writer) {
    for (ClassTypeInfo importedType : model.getImportedTypes()) {
      if (importedType.getKind() != ClassKind.API && !importedType.getPackageName().equals("java.lang")) {
        addImport(importedType, writer);
      }
    }
  }

  private void addImport(ClassTypeInfo type, PrintWriter writer) {
    writer.print("import ");
    writer.print(type.toString());
    writer.println(";");
  }

  private void startMethodTemplate(MethodInfo method, PrintWriter writer) {
    Doc doc = method.getDoc();
    if (doc != null) {
      writer.println("  /**");
      Token.toHtml(doc.getTokens(), "   *", this::renderLinkToHtml, "\n", writer);
      for (ParamInfo param : method.getParams()) {
        writer.print("   * @param ");
        writer.print(param.getName());
        writer.print(" ");
        if (param.getDescription() != null) {
          Token.toHtml(param.getDescription().getTokens(), "", this::renderLinkToHtml, "", writer);
        }
        writer.println();
      }
      if (!method.getReturnType().getName().equals("void")) {
        writer.print("   * @return ");
        if (method.getReturnDescription() != null) {
          Token.toHtml(method.getReturnDescription().getTokens(), "", this::renderLinkToHtml, "", writer);
        }
        writer.println();
      }
      if (method.isDeprecated()) {
        writer.print("   * @deprecated ");
        if (method.getDeprecatedDesc() != null) {
          writer.println(method.getDeprecatedDesc().getValue());
        } else {
          writer.println();
        }
      }
      writer.println("   */");
    }
    if (method.isDeprecated()) {
      writer.println("  @Deprecated()");
    }
    writer.print("  public ");
    if (method.isStaticMethod()) {
      writer.print("static ");
    }
    if (method.getTypeParams().size() > 0) {
      writer.print(method.getTypeParams().stream().map(TypeParamInfo::getName).collect(Collectors.joining(", ", "<", ">")));
      writer.print(" ");
    }
    writer.print(genTypeName(method.getReturnType()));
    writer.print(" ");
    writer.print(method.getName());
    writer.print("(");
    writer.print(method.getParams().stream().map(it -> genTypeName(it.getType()) + " " + it.getName()).collect(Collectors.joining(", ")));
    writer.print(")");

  }

  private String genOptTypeParamsDecl(ClassTypeInfo type, String deflt) {
    if (type.getParams().size() > 0) {
      return type.getParams().stream().map(TypeParamInfo::getName).collect(Collectors.joining(",", "<", ">"));
    } else {
      return deflt;
    }
  }
}
