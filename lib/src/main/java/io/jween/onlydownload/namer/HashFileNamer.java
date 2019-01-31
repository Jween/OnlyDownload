package io.jween.onlydownload.namer;

public class HashFileNamer implements FileNamer {
    @Override
    public String from(String url) {
        return String.valueOf(url.hashCode() );
    }
}
