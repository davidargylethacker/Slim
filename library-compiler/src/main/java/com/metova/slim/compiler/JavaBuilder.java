package com.metova.slim.compiler;

public class JavaBuilder {

    private final StringBuilder mClassBuilder = new StringBuilder();

    JavaBuilder(String packageName, String clsName, String[] interfaceNames) {
        mClassBuilder.append("// Generated code from Slim. Do not modify!\n");
        appendPackage(packageName);
        appendClass(clsName, interfaceNames);
    }

    private void appendPackage(String packageName) {
        mClassBuilder
                .append("package ")
                .append(packageName)
                .append(";");
    }

    private void appendClass(String clsName, String[] interfaceNames) {
        mClassBuilder
                .append("\n\npublic class ")
                .append(clsName);

        String interfaces = commaSeparatedStrings(interfaceNames);
        if (interfaces != null) {
            mClassBuilder
                    .append(" implements ")
                    .append(interfaces);
        }

        mClassBuilder.append(" {");
    }

    public JavaBuilder addMethod(String name, String[] params, String[] thrownExceptions, String body) {
        mClassBuilder
                .append("\n\n\tpublic void ")
                .append(name).append("(")
                .append(commaSeparatedStrings(params))
                .append(")");

        String exceptions = commaSeparatedStrings(thrownExceptions);
        if (exceptions != null && exceptions.length() > 0) {
            mClassBuilder.append(" throws ").append(exceptions);
        }

        mClassBuilder
                .append(" {\n")
                .append(body)
                .append("\n\t}");

        return this;
    }

    public String build() {
        return mClassBuilder.append("\n}").toString();
    }

    private String commaSeparatedStrings(String[] values) {
        if (values == null) {
            return null;
        }

        StringBuilder paramBuilder = new StringBuilder();
        for (String value : values) {
            if (paramBuilder.length() > 0) {
                paramBuilder.append(", ");
            }
            paramBuilder.append(value);
        }

        return paramBuilder.toString();
    }
}
