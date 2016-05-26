package org.aksw.twig.parsing;

import java.util.concurrent.Callable;

public class Twitter7BlockParser implements Callable<String> {

    private String lineT;

    private String lineU;

    private String lineW;

    public Twitter7BlockParser(String lineT, String lineU, String lineW) {
        this.lineT = lineT;
        this.lineU = lineU;
        this.lineW = lineW;
    }

    public String call() {
        StringBuilder builder = new StringBuilder();
        builder.append(lineT);
        builder.append('\n');
        builder.append(lineU);
        builder.append('\n');
        builder.append(lineW);
        builder.append('\n');
        return builder.toString();
    }
}
