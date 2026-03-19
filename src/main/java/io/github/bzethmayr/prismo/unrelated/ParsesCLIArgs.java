package io.github.bzethmayr.prismo.unrelated;

import java.util.Map;

public interface ParsesCLIArgs {
    Map<String, Object> getSymbols();
    String getSeparator();

    record DefinesCLIArgs(Map<String, Object> getSymbols, String getSeparator) implements ParsesCLIArgs {

        DefinesCLIArgs(Map<String, Object> getSymbols) {
            this(getSymbols, "--");
        }
    }
}
