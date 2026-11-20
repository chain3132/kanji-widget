package com.kanjiwidget.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Loads the kanji list from assets and remembers which one is showing. */
final class KanjiStore {

    static final int MODE_DAILY = 0;
    static final int MODE_HOURLY = 1;

    private static final String PREFS = "kanji_prefs";
    private static final String KEY_INDEX = "index";
    private static final String KEY_PERIOD = "period";
    private static final String KEY_MODE = "mode";
    private static final String KEY_CLEAR_BG = "clear_bg";

    private static List<Kanji> cache;

    private KanjiStore() {}

    static final class Kanji {
        String kanji, on, kun, meaning, word, wordReading, wordMeaning,
                sentence, sentenceReading, sentenceMeaning;

        /** e.g. "ひと ・ ジン・ニン" */
        String readingLine() {
            if (kun.isEmpty()) return on;
            if (on.isEmpty()) return kun;
            return kun + "  ・  " + on;
        }

        String wordLine() {
            return word + "（" + wordReading + "）";
        }
    }

    static synchronized List<Kanji> all(Context ctx) {
        if (cache != null) return cache;
        List<Kanji> list = new ArrayList<>();
        // assets/kanji.txt: one kanji per line, 10 fields separated by "|"
        // kanji|on|kun|meaning|word|word reading|word meaning|sentence|sentence reading|sentence meaning
        try (InputStream in = ctx.getAssets().open("kanji.txt")) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            String text = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            for (String line : text.split("\n")) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] f = line.split("\\|", -1);
                if (f.length < 10) continue;
                Kanji k = new Kanji();
                k.kanji = f[0].trim();
                k.on = f[1].trim();
                k.kun = f[2].trim();
                k.meaning = f[3].trim();
                k.word = f[4].trim();
                k.wordReading = f[5].trim();
                k.wordMeaning = f[6].trim();
                k.sentence = f[7].trim();
                k.sentenceReading = f[8].trim();
                k.sentenceMeaning = f[9].trim();
                list.add(k);
            }
            if (list.isEmpty()) throw new IllegalStateException("no kanji");
        } catch (Exception e) {
            list.clear();
            Kanji k = new Kanji();
            k.kanji = "人"; k.on = "ジン"; k.kun = "ひと"; k.meaning = "คน";
            k.word = "日本人"; k.wordReading = "にほんじん"; k.wordMeaning = "คนญี่ปุ่น";
            k.sentence = "あの人は日本人です。"; k.sentenceReading = "あのひとはにほんじんです。";
            k.sentenceMeaning = "คนคนนั้นเป็นคนญี่ปุ่น";
            list.add(k);
        }
        cache = Collections.unmodifiableList(list);
        return cache;
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static int index(Context ctx) {
        int size = all(ctx).size();
        int i = prefs(ctx).getInt(KEY_INDEX, 0);
        return ((i % size) + size) % size;
    }

    static Kanji current(Context ctx) {
        return all(ctx).get(index(ctx));
    }

    static void setIndex(Context ctx, int i) {
        int size = all(ctx).size();
        prefs(ctx).edit()
                .putInt(KEY_INDEX, ((i % size) + size) % size)
                .putLong(KEY_PERIOD, periodKey(ctx))
                .apply();
    }

    static void step(Context ctx, int delta) {
        setIndex(ctx, index(ctx) + delta);
    }

    /** Moves to the next kanji once a new day (or hour) has started. */
    static void advanceIfDue(Context ctx) {
        SharedPreferences p = prefs(ctx);
        long now = periodKey(ctx);
        long last = p.getLong(KEY_PERIOD, -1);
        if (last == -1) {
            p.edit().putLong(KEY_PERIOD, now).apply();
        } else if (now != last) {
            int size = all(ctx).size();
            p.edit()
                    .putInt(KEY_INDEX, (index(ctx) + 1) % size)
                    .putLong(KEY_PERIOD, now)
                    .apply();
        }
    }

    private static long periodKey(Context ctx) {
        if (mode(ctx) == MODE_HOURLY) {
            return LocalDate.now().toEpochDay() * 24
                    + java.time.LocalTime.now().getHour();
        }
        return LocalDate.now().toEpochDay();
    }

    static int mode(Context ctx) {
        return prefs(ctx).getInt(KEY_MODE, MODE_DAILY);
    }

    static void setMode(Context ctx, int mode) {
        prefs(ctx).edit().putInt(KEY_MODE, mode).apply();
        prefs(ctx).edit().putLong(KEY_PERIOD, periodKey(ctx)).apply();
    }

    static boolean clearBackground(Context ctx) {
        return prefs(ctx).getBoolean(KEY_CLEAR_BG, false);
    }

    static void setClearBackground(Context ctx, boolean clear) {
        prefs(ctx).edit().putBoolean(KEY_CLEAR_BG, clear).apply();
    }
}
