package com.orange.ngsi2.model;

import java.util.List;

/**
 * Wrapper for a list of paginated items T
 */
public class Paginated<T> {

    private List<T> items;

    private int offset;

    private int limit;

    private int total;

    public Paginated(List<T> items, int offset, int limit, int count) {
        this.items = items;
        this.offset = offset;
        this.limit = limit;
        this.total = count;
    }

    public List<T> getItems() {
        return items;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public int getTotal() {
        return total;
    }
}
