package com.igormaznitsa.mvngolang;

import java.io.File;

public class SpyGolangBuildMojo extends GolangBuildMojo {
    @Override
    protected void afterExecutionResultFile(File resultFile) {
        // ignore
    }
}
