package com.pizzaparker07.spacebreaker.core.astro;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiny JSON reader for the tests (the unit-test classpath has no game libraries). */
final class MiniJson {
    private final String s;
    private int i;

    private MiniJson(String s) {
        this.s = s;
    }

    static Object parse(String text) {
        MiniJson p = new MiniJson(text);
        Object v = p.value();
        p.ws();
        if (p.i != p.s.length()) {
            throw new IllegalArgumentException("trailing data at " + p.i);
        }
        return v;
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
    }

    private Object value() {
        ws();
        char c = s.charAt(i);
        if (c == '{') {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            ws();
            if (s.charAt(i) == '}') {
                i++;
                return m;
            }
            while (true) {
                ws();
                String k = string();
                ws();
                expect(':');
                m.put(k, value());
                ws();
                if (s.charAt(i) == ',') {
                    i++;
                } else {
                    expect('}');
                    return m;
                }
            }
        }
        if (c == '[') {
            List<Object> l = new ArrayList<>();
            i++;
            ws();
            if (s.charAt(i) == ']') {
                i++;
                return l;
            }
            while (true) {
                l.add(value());
                ws();
                if (s.charAt(i) == ',') {
                    i++;
                } else {
                    expect(']');
                    return l;
                }
            }
        }
        if (c == '"') {
            return string();
        }
        if (s.startsWith("true", i)) {
            i += 4;
            return Boolean.TRUE;
        }
        if (s.startsWith("false", i)) {
            i += 5;
            return Boolean.FALSE;
        }
        if (s.startsWith("null", i)) {
            i += 4;
            return null;
        }
        int st = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
            i++;
        }
        if (st == i) {
            throw new IllegalArgumentException("unexpected '" + c + "' at " + i);
        }
        return Double.parseDouble(s.substring(st, i));
    }

    private String string() {
        expect('"');
        StringBuilder b = new StringBuilder();
        while (s.charAt(i) != '"') {
            char c = s.charAt(i++);
            if (c == '\\') {
                char n = s.charAt(i++);
                b.append(switch (n) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    case 'u' -> {
                        char u = (char) Integer.parseInt(s.substring(i, i + 4), 16);
                        i += 4;
                        yield u;
                    }
                    default -> n;
                });
            } else {
                b.append(c);
            }
        }
        i++;
        return b.toString();
    }

    private void expect(char c) {
        if (s.charAt(i) != c) {
            throw new IllegalArgumentException("expected '" + c + "' at " + i + " got '" + s.charAt(i) + "'");
        }
        i++;
    }
}
