package fr.ippon.tatami.domain;

/**
 * Created by lnorregaard on 26/01/16.
 */
public class Ping {

    private long cassandra;
    private long elasticSearch;

    public long getCassandra() {
        return cassandra;
    }

    public void setCassandra(long cassandra) {
        this.cassandra = cassandra;
    }

    public long getElasticSearch() {
        return elasticSearch;
    }

    public void setElasticSearch(long elasticSearch) {
        this.elasticSearch = elasticSearch;
    }
}
