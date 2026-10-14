package app;

import lib.Lib;

public final class App {
    private App() {}

    public static String describe() {
        return "app(" + Lib.name() + ")";
    }
}
