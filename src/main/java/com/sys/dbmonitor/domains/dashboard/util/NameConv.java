// src/main/java/com/sys/dbmonitor/domains/dashboard/util/NameConv.java
package com.sys.dbmonitor.domains.dashboard.util;

/** SNAKE_CASE → camelCase, 그리고 일부 특수키 별칭 처리 */
public final class NameConv {
    private NameConv(){}

    public static String snakeToCamel(String col) {
        String s = col.toLowerCase();
        String[] parts = s.split("_");
        StringBuilder b = new StringBuilder(parts[0]);
        for (int i=1;i<parts.length;i++){
            if (parts[i].isEmpty()) continue;
            b.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length()>1) b.append(parts[i].substring(1));
        }
        return b.toString();
    }
}
