package common;

public interface RateLimit {

    long INTERNAL = 60 * 1000;// ms
    long LIMIT = 1000000;

    boolean grant();
}
