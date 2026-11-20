package com.kanjiwidget.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button prev = findViewById(R.id.btn_prev);
        Button next = findViewById(R.id.btn_next);
        Button add = findViewById(R.id.btn_add_widget);
        Button listBtn = findViewById(R.id.btn_list);
        RadioGroup modeGroup = findViewById(R.id.mode_group);
        Switch clearBg = findViewById(R.id.switch_clear_bg);

        prev.setOnClickListener(v -> { KanjiStore.step(this, -1); refresh(); });
        next.setOnClickListener(v -> { KanjiStore.step(this, 1); refresh(); });
        findViewById(R.id.kanji).setOnClickListener(v -> { KanjiStore.step(this, 1); refresh(); });

        modeGroup.check(KanjiStore.mode(this) == KanjiStore.MODE_HOURLY
                ? R.id.mode_hourly : R.id.mode_daily);
        modeGroup.setOnCheckedChangeListener((g, id) ->
                KanjiStore.setMode(this, id == R.id.mode_hourly
                        ? KanjiStore.MODE_HOURLY : KanjiStore.MODE_DAILY));

        clearBg.setChecked(KanjiStore.clearBackground(this));
        clearBg.setOnCheckedChangeListener((b, checked) -> {
            KanjiStore.setClearBackground(this, checked);
            refresh();
        });

        add.setOnClickListener(v -> requestPinWidget());
        listBtn.setOnClickListener(v -> showList());
    }

    @Override
    protected void onResume() {
        super.onResume();
        KanjiStore.advanceIfDue(this);
        refresh();
    }

    private void refresh() {
        KanjiStore.Kanji k = KanjiStore.current(this);
        setText(R.id.kanji, k.kanji);
        setText(R.id.reading, k.readingLine());
        setText(R.id.meaning, k.meaning);
        setText(R.id.word, k.wordLine());
        setText(R.id.word_meaning, k.wordMeaning);
        setText(R.id.sentence, k.sentence);
        setText(R.id.sentence_reading, k.sentenceReading);
        setText(R.id.sentence_meaning, k.sentenceMeaning);

        View card = findViewById(R.id.widget_root);
        card.setBackgroundResource(KanjiStore.clearBackground(this)
                ? R.drawable.preview_wallpaper : R.drawable.widget_bg_dark);

        TextView counter = findViewById(R.id.counter);
        counter.setText((KanjiStore.index(this) + 1) + " / " + KanjiStore.all(this).size());

        KanjiWidgetProvider.updateAll(this);
    }

    private void setText(int id, CharSequence text) {
        ((TextView) findViewById(id)).setText(text);
    }

    private void requestPinWidget() {
        AppWidgetManager manager = getSystemService(AppWidgetManager.class);
        ComponentName provider = new ComponentName(this, KanjiWidgetProvider.class);
        if (manager != null && manager.isRequestPinAppWidgetSupported()) {
            manager.requestPinAppWidget(provider, null, null);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.how_to_title)
                    .setMessage(R.string.how_to_body)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void showList() {
        List<KanjiStore.Kanji> all = KanjiStore.all(this);
        String[] items = new String[all.size()];
        for (int i = 0; i < all.size(); i++) {
            KanjiStore.Kanji k = all.get(i);
            items[i] = (i + 1) + ".  " + k.kanji + "   " + k.meaning;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.all_kanji)
                .setItems(items, (d, which) -> {
                    KanjiStore.setIndex(this, which);
                    refresh();
                    Toast.makeText(this, all.get(which).kanji, Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
