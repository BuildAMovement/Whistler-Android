package rs.readahead.washington.mobile.views.collect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryCaption;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.collect.widgets.WidgetFactory;


@SuppressLint("ViewConstructor")
public class CollectFormView extends LinearLayout {
    public static final String FIELD_LIST = "field-list";

    // starter random number for view IDs
    private static final int VIEW_ID = 12061974;

    private ArrayList<QuestionWidget> widgets;


    public CollectFormView(Context context, final FormEntryPrompt[] questionPrompts, FormEntryCaption[] groups) {
        super(context);

        widgets = new ArrayList<>();

        inflate(context, R.layout.collect_form_view, this);

        // set my layout
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutParams);
        setOrientation(VERTICAL);

        // set padding
        int h = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        int v = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
        setPadding(h, v, h, v);

        TextView formViewTitle = (TextView) findViewById(R.id.formViewTitle);
        formViewTitle.setText(getGroupTitle(groups));

        // prepare widgets layout
        LinearLayout.LayoutParams widgetLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        // when the grouped fields are populated by an external app, this will get true.
        boolean readOnlyOverride = false;

        // external app handling removed..

        int id = 0;
        for (FormEntryPrompt p: questionPrompts) {
            QuestionWidget qw = WidgetFactory.createWidgetFromPrompt(p, getContext(), readOnlyOverride);
            if (qw != null) {
                qw.setId(VIEW_ID + id++);
                widgets.add(qw);
                addView(qw, widgetLayout);
            }
        }
    }

    public void setFocus(Context context) {
        if (widgets.size() > 0) {
            widgets.get(0).setFocus(context);
        }
    }

    public void setValidationConstraintText(FormIndex formIndex, String text) {
        for (QuestionWidget q: widgets) {
            if (q.getPrompt().getIndex() == formIndex) {
                q.setConstraintValidationText(text);
                break;
            }
        }
    }

    public void clearValidationConstraints() {
        for (QuestionWidget q: widgets) {
            q.setConstraintValidationText(null);
        }
    }

    public LinkedHashMap<FormIndex, IAnswerData> getAnswers() {
        LinkedHashMap<FormIndex, IAnswerData> answers = new LinkedHashMap<>();

        for (QuestionWidget q: widgets) {
            FormEntryPrompt p = q.getPrompt();
            answers.put(p.getIndex(), q.getAnswer());
        }

        return answers;
    }

    private String getGroupTitle(FormEntryCaption[] groups) {
        StringBuilder s = new StringBuilder("");
        String t = "";
        int i;

        // list all groups in one string
        for (FormEntryCaption g : groups) {
            i = g.getMultiplicity() + 1;
            t = g.getLongText();
            if (t != null) {
                s.append(t);
                if (g.repeats() && i > 0) {
                    s.append(" (" + i + ")");
                }
                s.append(" > ");
            }
        }

        return s.length() > 0 ? s.substring(0, s.length() - 3) : s.toString();
    }
}
