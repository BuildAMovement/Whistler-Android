/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package rs.readahead.washington.mobile.views.collect.widgets;

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TimePicker;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.form.api.FormEntryPrompt;
import org.joda.time.DateTime;

import java.util.Date;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.CommonUtils;


/**
 * Based on ODK TimeWidget.
 */
public class TimeWidget extends QuestionWidget {
    private TimePickerDialog timePickerDialog;

    private Button timeButton;

    private int hour;
    private int minute;

    private boolean nullAnswer = false;


    public TimeWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);

        createTimePickerDialog();

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        addTimeButton(linearLayout);

        if (formEntryPrompt.getAnswerValue() == null) {
            clearAnswer();
        } else {
            DateTime dt = new DateTime(((Date) formEntryPrompt.getAnswerValue().getValue()).getTime());

            hour = dt.getHourOfDay();
            minute = dt.getMinuteOfHour();

            setWidgetTime();
            timePickerDialog.updateTime(hour, minute);
        }

        addAnswerView(linearLayout);
    }

    @Override
    public void clearAnswer() {
        nullAnswer = true;
        timeButton.setText(getResources().getString(R.string.ra_select_time)); // todo: say something smart here..
    }

    @Override
    public IAnswerData getAnswer() {
        clearFocus();

        if (nullAnswer) {
            return null;
        }

        // use picker time, convert to today's date, store as utc
        DateTime dt = (new DateTime()).withTime(hour, minute, 0, 0);

        return new TimeData(dt.toDate());
    }

    @Override
    public void setFocus(Context context) {
        CommonUtils.hideKeyboard(context, this);
    }

    private void setWidgetTime() {
        nullAnswer = false;
        timeButton.setText(getAnswer().getDisplayText());
    }

    private void createTimePickerDialog() {
        timePickerDialog = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                TimeWidget.this.hour = hourOfDay;
                TimeWidget.this.minute = minute;

                setWidgetTime();
            }
        }, 0, 0, DateFormat.is24HourFormat(getContext()));
    }

    private void addTimeButton(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        inflater.inflate(R.layout.collect_widget_time, linearLayout, true);

        timeButton = (Button) linearLayout.findViewById(R.id.timeWidgetButton);
        timeButton.setId(QuestionWidget.newUniqueId());
        timeButton.setText(getResources().getString(R.string.ra_select_time));
        timeButton.setEnabled(!formEntryPrompt.isReadOnly());

        timeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nullAnswer) {
                    DateTime dt = new DateTime();
                    timePickerDialog.updateTime(dt.getHourOfDay(), dt.getMinuteOfHour());
                }

                timePickerDialog.show();
            }
        });
    }
}
