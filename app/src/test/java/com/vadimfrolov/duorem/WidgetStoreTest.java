package com.vadimfrolov.duorem;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WidgetStoreTest {
    @Test
    public void monitoringIntervalIsConstrainedToSupportedValues() {
        assertEquals(0, WidgetStore.normalizeInterval(0));
        assertEquals(30, WidgetStore.normalizeInterval(30));
        assertEquals(60, WidgetStore.normalizeInterval(60));
        assertEquals(300, WidgetStore.normalizeInterval(300));
        assertEquals(600, WidgetStore.normalizeInterval(600));
        assertEquals(1800, WidgetStore.normalizeInterval(1800));
        assertEquals(3600, WidgetStore.normalizeInterval(3600));
        assertEquals(7200, WidgetStore.normalizeInterval(7200));
        assertEquals(1800, WidgetStore.normalizeInterval(5));
    }
}
