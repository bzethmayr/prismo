package io.github.bzethmayr.prismo.unrelated;

import io.github.bzethmayr.prismo.unrelated.RepresentsCLIArgs.ImmutableCLIArgs;
import io.github.bzethmayr.prismo.unrelated.RepresentsCLIArgs.MutableCLIArgs;

import java.util.LinkedList;

public class CLIArgs {

    public static RepresentsCLIArgs parseCLIArgs(final ParsesCLIArgs definition, final String[] tokens) {
        boolean separated = false;
        final MutableCLIArgs mutable = new MutableCLIArgs();
        Object lastOption = null;
        for (final String token : tokens) {
            if (token.equals(definition.getSeparator())) {
                separated = true;
                continue;
            }
            final Object thisOption = definition.getSymbols().get(token);
            if (thisOption != null) {
                lastOption = thisOption;
                continue;
            }
            if (separated || lastOption == null) {
                mutable.getArguments().add(token);
            } else {
                mutable.getOptions().computeIfAbsent(lastOption, k -> new LinkedList<>()).add(token);
            }
        }
        return new ImmutableCLIArgs(mutable);
    }
}
