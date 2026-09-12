package com.cloudide.cloudide.enums;

public enum ProgrammingLanguage {
    JAVA,
    PYTHON,
    C,
    CPP;

    public static ProgrammingLanguage fromString(String text) {
        if (text == null) {
            return JAVA;
        }
        for (ProgrammingLanguage lang : ProgrammingLanguage.values()) {
            if (lang.name().equalsIgnoreCase(text.trim())) {
                return lang;
            }
        }
        return JAVA;
    }
}
