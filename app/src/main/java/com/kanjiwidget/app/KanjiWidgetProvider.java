package com.kanjiwidget.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Home-screen widget: shows one kanji card; tap the kanji for the next one. */
public class KanjiWidgetProvider extends AppWidgetProvider {

    static final String ACTION_NEXT = "com.kanjiwidget.app.ACTION_NEXT";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager manager, int[] ids) {
        KanjiStore.advanceIfDue(ctx);
        RemoteViews views = build(ctx);
        for (int id : ids) manager.updateAppWidget(id, views);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (ACTION_NEXT.equals(intent.getAction())) {
            KanjiStore.step(ctx, 1);
            updateAll(ctx);
        }
    }

    /** Redraws every placed widget. */
    static void updateAll(Context ctx) {
        AppWidgetManager manager = AppWidgetManager.getInstance(ctx);
        int[] ids = manager.getAppWidgetIds(new ComponentName(ctx, KanjiWidgetProvider.class));
        if (ids == null || ids.length == 0) return;
        RemoteViews views = build(ctx);
        for (int id : ids) manager.updateAppWidget(id, views);
    }

    static RemoteViews build(Context ctx) {
        KanjiStore.Kanji k = KanjiStore.current(ctx);
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_kanji);

        v.setTextViewText(R.id.kanji, k.kanji);
        v.setTextViewText(R.id.reading, k.readingLine());
        v.setTextViewText(R.id.meaning, k.meaning);
        v.setTextViewText(R.id.word, k.wordLine());
        v.setTextViewText(R.id.word_meaning, k.wordMeaning);
        v.setTextViewText(R.id.sentence, k.sentence);
        v.setTextViewText(R.id.sentence_reading, k.sentenceReading);
        v.setTextViewText(R.id.sentence_meaning, k.sentenceMeaning);

        v.setInt(R.id.widget_root, "setBackgroundResource",
                KanjiStore.clearBackground(ctx) ? R.drawable.widget_bg_clear : R.drawable.widget_bg_dark);

        // Tap the big kanji -> next kanji
        Intent next = new Intent(ctx, KanjiWidgetProvider.class).setAction(ACTION_NEXT);
        PendingIntent nextPi = PendingIntent.getBroadcast(ctx, 1, next,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.kanji, nextPi);

        // Tap anywhere else -> open the app
        Intent open = new Intent(ctx, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(ctx, 2, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.widget_root, openPi);

        return v;
    }
}
