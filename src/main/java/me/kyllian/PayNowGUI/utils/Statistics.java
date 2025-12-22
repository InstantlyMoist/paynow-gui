package me.kyllian.PayNowGUI.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class Statistics {

    public static int products = 0;
    public static int tags = 0;

    public static AtomicInteger menuOpened = new AtomicInteger(0);
    public static AtomicInteger cartsOpened = new AtomicInteger(0);
    public static AtomicInteger lunarCartsOpened = new AtomicInteger(0);
    public static AtomicInteger cartsCleared = new AtomicInteger(0);

    public static AtomicInteger productsAdded = new AtomicInteger(0);
    public static AtomicInteger productsRemoved = new AtomicInteger(0);

    public static int getAndReset(AtomicInteger counter) {
        return counter.getAndSet(0);
    }
}
