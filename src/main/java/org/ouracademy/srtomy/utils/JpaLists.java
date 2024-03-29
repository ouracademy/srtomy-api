package org.ouracademy.srtomy.utils;

import java.util.List;

public class JpaLists {

    /**
     * In jpa, we can't setMyList() assign directly
     * this.myList = newMyList;
     * 
     * As it will replace the proxy being used by JPA
     * 
     * @param <T>
     * @param list
     * @param newList
     */
    public static <T> void set(List<T> list, List<T> newList) {
        list.retainAll(newList);
        list.addAll(newList);
    }
}
