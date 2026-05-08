package io.github.bzethmayr.prismo.model;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public interface TestsWithIterationStats {
    default IterationStats fakeStats(final boolean tagged) {
        final IterationStats fake = mock();
        if (tagged) {
            doAnswer(iom -> {
                final IterationVariable<?> withered = mock();
                doReturn(iom.getArgument(0)).when(withered).value();
                doReturn(iom.getArgument(1)).when(withered).tag();
                return withered;
            }).when(fake).withValue(any(), any());
        } else {
            doAnswer(iom -> {
                final IterationVariable<?> withered = mock();
                doReturn(iom.getArgument(0)).when(withered).value();
                return withered;
            }).when(fake).withValue(any());
        }
        return fake;
    }

}
